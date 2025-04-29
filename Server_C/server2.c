#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>      // for close()
#include <arpa/inet.h>   // for socket functions
#include <pthread.h>    // threads
#include <netdb.h>
#include <sys/select.h>
#include <stdbool.h>

#define NDEBUG
#include <assert.h>
#include <termios.h> // termial informaion
#include <fcntl.h> // for non blocking flags


#define PORT 8080
#define BUFFER_SIZE 1024

#define BOARD_SIZE_X 10
#define BOARD_SIZE_Y 20

#define MAX_PLAYER_NUMBER 16
#define MAX_QUEUE_SIZE 64

typedef enum player_status {
    joining_lobby_s,
    ready_s,
    not_ready_s,
    playing_s,
    lost_s,
} player_status;

typedef struct score_t {
    int current_score;
    float game_state;
    int lines_scored;
} score_t;

typedef struct player_t {
    score_t score;
    char** board; // board[][]
    player_status status;
    char player_mark;
} player_t;

typedef struct thread_listener_t thread_listener_t;

typedef struct exchange_information_handler_t{
    thread_listener_t* thread_arr; // thread_arr[]
    char current_player_num;
} exchange_information_handler_t;


typedef enum task_type {
    send_score,
    send_updated_board,
    send_lines
} task_type;

typedef struct thread_task_t {
    task_type type;
    void* data; // pointer to corresponding data from task_type
    pthread_mutex_t* that_thread_lock;
} thread_task_t;

typedef struct task_queue_t {
    thread_task_t array[MAX_QUEUE_SIZE];
    int current_size;
} task_queue_t;


typedef struct thread_listener_t {
    player_t player_data; 

    exchange_information_handler_t* handler;
    int my_player_index;

    pthread_mutex_t my_lock;
    task_queue_t my_queue;
    int client_fd;
} thread_listener_t;


typedef struct server_t {
    int server_fd;
    struct sockaddr_in address;
    int address_len;    
    
    exchange_information_handler_t handler;
    int cur_player_num;

    pthread_t thread_handles[MAX_PLAYER_NUMBER];
    thread_listener_t* threads;
} server_t;

typedef enum message_type {
    not_ok_m = (char) 0,
    ok_m = (char) 1,
    player_ready_m = (char) 2,
    player_not_ready_m = (char) 3,
    get_other_players_m = (char) 4,
}message_type;

bool start_game = false;

score_t* get_other_player_score(int my_index, exchange_information_handler_t* h,char player_signs[MAX_PLAYER_NUMBER]) {
    score_t* result = (score_t*) malloc(sizeof(score_t) * h->current_player_num);
    
    for(int i = 0; i < h->current_player_num; i++) {
        result[i] = h->thread_arr[i].player_data.score;
        player_signs[i] = h->thread_arr[i].player_data.player_mark;
    }
    return result;
} 


int send_other_players_score(thread_listener_t* l) {
    
    //those variables should not change types
    assert(sizeof(l->player_data.score.current_score) == sizeof(int));
    assert(sizeof(l->player_data.score.game_state) == sizeof(float));
    assert(sizeof(l->player_data.score.lines_scored) == sizeof(int));
    assert(sizeof(l->handler->current_player_num) == sizeof(char));
    
    // send order in binary:
    // first bytes:(int)bytes_to_read (char) player_num 
    // rest: (char)player_mark (int)current_score (float)game_state (int)lines_scored
    
    char player_signs[MAX_PLAYER_NUMBER] = {0};
    score_t* scores = get_other_player_score(l->my_player_index, l->handler, player_signs);
    
    char buffer[BUFFER_SIZE] = {0};
    
    int start_offset = sizeof(int) + sizeof(char);

    for(int i = 0 ;i < l->handler->current_player_num; i++) { 
        buffer[start_offset] = player_signs[i];
        start_offset++;
        memcpy(buffer + start_offset, &scores[i], sizeof(score_t));
        start_offset += sizeof(score_t);
    }
    
    memcpy(buffer, &start_offset, sizeof(int)); // start_offset is also a size of message
    buffer[sizeof(int)] =  l->handler->current_player_num;
    send(l->client_fd, buffer,start_offset,0);
    free(scores);
}

int lobby_loop(thread_listener_t* l) {
    // to prevent potential buffer overflow critical vulnerability    
    char* buffer = (char*) malloc(sizeof(char) * BUFFER_SIZE);

    while(!start_game) {
        int bytes_read = read(l->client_fd, buffer, BUFFER_SIZE);
        if (bytes_read <= 0) {       
            return -1;
        }
        switch (buffer[0])
        {
        case player_ready_m:
            printf("player: %c is ready\n", l->player_data.player_mark);
            l->player_data.status = ready_s;
            message_type type1 = ok_m;
            send(l->client_fd,&type1,1,0);
            break; 
        case player_not_ready_m: 
            printf("player: %c is not ready\n", l->player_data.player_mark);
            l->player_data.status = not_ready_s;
            message_type type2 = ok_m;
            send(l->client_fd,&type2,1,0);
            break;
        case get_other_players_m:
            printf("player: %c want other players\n", l->player_data.player_mark);
            send_other_players_score(l);
            break; 
        default:
            printf("wrong message sent: %c\n", l->player_data.player_mark);
            break;
        }
    }
    free(buffer);
}

void connect_to_lobby(thread_listener_t* l) {

    char buffer[BUFFER_SIZE] = "\0";
    bool player_in_lobby = false;
    while(!player_in_lobby) {
        int bytes_read = read(l->client_fd, buffer, BUFFER_SIZE);
        
        if (bytes_read <= 0) {
            break;
        }
        if (strcmp(buffer, "exit") == 0){
            break;
        }
        if (strcmp(buffer, "connect to lobby") != 0) {
            printf("Wrong message sent\n");
            continue;
        }

        // Send to player its mark
        buffer[0]=l->player_data.player_mark;
        send(l->client_fd, buffer, 1, 0);

        printf("Sent to player its sign: %c\n", l->player_data.player_mark);
        player_in_lobby = true;
        l->player_data.status = not_ready_s;
    };
}

void* client_listener(void* arg) {
    thread_listener_t* listener = (thread_listener_t*)arg;
    
    printf("I started working %c \n", listener->player_data.player_mark);
    
    connect_to_lobby(listener);

    lobby_loop(listener);
}

int connection_wait(server_t* s, int* cur_player_count) {
    
    fd_set readfds;
    FD_ZERO(&readfds);
    FD_SET(s->server_fd, &readfds);

    struct timeval timeout = {3,0}; // 3.0 seconds timeout

    int ready = select(s->server_fd +1,&readfds,NULL, NULL, &timeout);    

    if(ready < 0) {
        perror("select error");
        return -1;
    }else if(ready == 0) {
        //timeout occured
        return 0;
    }

    int client_fd = 0;
    // waiting for connection
    if ((client_fd = accept(s->server_fd, (struct sockaddr*)& (s->address),(socklen_t*)& (s->address_len))) < 0) { 
        perror("accept failed");
        close(s->server_fd);
        exit(EXIT_FAILURE);
    }
    printf("Player accepted\n");
    
    s->threads[*cur_player_count].client_fd = client_fd;

    pthread_create(&(s->thread_handles[*cur_player_count]),NULL,client_listener, &(s->threads[*cur_player_count]));        
    (*cur_player_count)++;
    s->cur_player_num = *cur_player_count;
    s->handler.current_player_num = *cur_player_count;

    return 1;
}

bool all_players_ready(server_t* s) {
    if(s->cur_player_num == 0) {
        return false;
    }

    for(int i =0; i < s->cur_player_num; i++) {
        if(s->threads[i].player_data.status != ready_s) {
            return false;        
        }
    }
    return true;
}

bool keyboard_shutdown() {

    //looking for keyboard interrupt
    char ch = 0; 
    int result = read(STDERR_FILENO,&ch,1);
    if (result > 0) {
        if(ch=='u'){
            printf("closing server\n");
            return true;
        }
    } 
    return false;
}

void listen_for_connections(server_t* s) {
   
    //setting up non blocking accept clients
    int flags = fcntl(s->server_fd,F_GETFL,0); //file descriptior settings F_GETFL = get current flags
    fcntl(s->server_fd,F_SETFL,flags | O_NONBLOCK);

    printf("Waiting for players to join\n");
    int cur_player_count = 0;
    while (cur_player_count < MAX_PLAYER_NUMBER) { // waiting players to connect 
        if(keyboard_shutdown()) {
            break;
        }

        if(connection_wait(s, &cur_player_count)== - 1) {
            break;  
        }

        // check if all lobby players are ready
        if(all_players_ready(s)) {
            printf("all players are ready!\n");
            start_game = true;
            break;
        }        
    }
}
void init_player_data(server_t* s) {
    
    //player board init;
    char ***player_boards;
    player_boards = (char***)malloc(sizeof(char*)*MAX_PLAYER_NUMBER);
    
    for(int z = 0; z < MAX_PLAYER_NUMBER;z++) {
        player_boards[z] = (char**) malloc(sizeof(char*)*BOARD_SIZE_Y);
        for(int y = 0; y<BOARD_SIZE_Y;y++) {
            player_boards[z][y] = (char*)malloc(sizeof(char)*BOARD_SIZE_X);
            for(int x = 0; x<BOARD_SIZE_X;x++) {
                player_boards[z][y][x] = '1' + x;
            }    
        }
    }


    //thread array
    thread_listener_t* threads = (thread_listener_t*)malloc(sizeof(thread_listener_t)*MAX_PLAYER_NUMBER);

    exchange_information_handler_t handler;
    handler.current_player_num = 0;
    handler.thread_arr = threads;

    s->handler = handler;
    
    for(int i = 0; i<MAX_PLAYER_NUMBER;i++) {
        //internal data
        //lock        
        pthread_mutex_t lock;
        pthread_mutex_init(&lock,NULL);
        threads[i].my_lock = lock;
        
        //index
        threads[i].my_player_index = i;    
        
        //handler
        threads[i].handler = &(s->handler);
        
        // queue
        threads[i].my_queue.current_size = 0;
        
        //player data
        score_t score;
        score.current_score = 0;
        score.game_state = 0.0;
        score.lines_scored = 0;

        player_status status = joining_lobby_s;

        player_t player_data;
        player_data.board = player_boards[i];
        player_data.score = score;
        player_data.status = status;
        player_data.player_mark = 'A' + i;

        threads[i].player_data = player_data;
    }
    s->threads = threads;     
}

void init_server(server_t* s) {
    // Create socket file descriptor
    if ((s->server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
        perror("socket failed");
        exit(EXIT_FAILURE);
    }

    // Bind to port
    s->address.sin_family = AF_INET;
    s->address.sin_addr.s_addr = INADDR_ANY; // <- listening on this port
    s->address.sin_port = htons(PORT);
    s->address_len = sizeof(struct sockaddr_in);


    int opt = 1;
    setsockopt(s->server_fd,SOL_SOCKET,SO_REUSEADDR,&opt, sizeof(opt)); // setting up reusing sockets

    if (bind(s->server_fd, (struct sockaddr*) &(s->address), sizeof(s->address)) < 0) {
        perror("bind failed");
        close(s->server_fd);
        exit(EXIT_FAILURE);
    }

    // Listen for connections
    if (listen(s->server_fd, 3) < 0) {
        perror("listen failed");
        close(s->server_fd);
        exit(EXIT_FAILURE);
    }
    printf("Server is listening on port %d...\n", PORT);

    // printing server ip
    char hostname[128];
    gethostname(hostname, sizeof(hostname));
    struct hostent *host_entry = gethostbyname(hostname);
    char *IPbuffer = inet_ntoa(*((struct in_addr*) host_entry->h_addr_list[0]));

    printf("Server IP: %s\n", IPbuffer); 

    s->cur_player_num = 0;
}

// Set terminal to non-canonical, non-blocking mode
void set_nonblocking_input() {
    struct termios ttystate;
    
    // get terminal state
    tcgetattr(STDIN_FILENO, &ttystate);         
    ttystate.c_lflag &= ~ICANON;                
    ttystate.c_lflag &= ~ECHO;                  
    tcsetattr(STDIN_FILENO, TCSANOW, &ttystate);
    
    // non-blocking read
    fcntl(STDIN_FILENO, F_SETFL, O_NONBLOCK);   
}

// Reset terminal to default state
void reset_input_mode() {
    struct termios ttystate;
    tcgetattr(STDIN_FILENO, &ttystate);
    ttystate.c_lflag |= ICANON;
    ttystate.c_lflag |= ECHO;
    tcsetattr(STDIN_FILENO, TCSANOW, &ttystate);
}

int main(){
    set_nonblocking_input();
    
    server_t server;
    init_server(&server);
    init_player_data(&server);
    listen_for_connections(&server);

    close(server.server_fd);
    reset_input_mode();
}