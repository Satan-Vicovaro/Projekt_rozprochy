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
    not_ready_s = 0,
    ready_s = 1,
    joining_lobby_s = 2,
    playing_s = 3,
    lost_s = 4,
    error_s = 5,
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
    send_score_task = 1,
    send_updated_board_task = 2,
    send_lines_task = 3,
    send_player_status_task = 4,
} task_type;

typedef struct thread_task_t {
    task_type type;
    void* data; // pointer to corresponding data from task_type
    pthread_mutex_t* that_thread_lock;
    char player_mark;
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
    update_board_m = (char) 5,
    update_score_m = (char) 6,
    send_lines_to_enemy_m = (char) 7,
    start_game_m = (char) 8,
    not_a_message_m =(char) 9,
    message_timeout_m = (char) 10,
    player_status_m =(char)11
}message_type;

bool start_game = false;
bool global_game_over = false;

void send_to_one(exchange_information_handler_t* h,int receiver_index, thread_task_t new_task) { 
    
    if(receiver_index < 0 || receiver_index > h->current_player_num-1) {
        printf("%c: Wrong receiver_index: %d\n", new_task.player_mark, receiver_index);
        return;
    }

    thread_listener_t* thread = &(h->thread_arr[receiver_index]);
    pthread_mutex_t* lock  = &(thread->my_lock);     

    if(thread->my_queue.current_size > MAX_QUEUE_SIZE) {
        printf("Could not add to queue: %c\n",thread->player_data.player_mark );
        return;
    }

    //adding data    
    pthread_mutex_lock(lock);
    int size = (thread->my_queue.current_size);
    thread->my_queue.array[size] = new_task;
    (thread->my_queue.current_size)++;
    pthread_mutex_unlock(lock);
}

void send_to_all_besides_me(exchange_information_handler_t* h, int my_index, thread_task_t new_task) {
    for(int i = 0; i < h->current_player_num; i++) {
        if(i == my_index) {
            //skipping ourselves
            //continue;
        }
        
        thread_listener_t* thread = &(h->thread_arr[i]);
        pthread_mutex_t* lock  = &(thread->my_lock);     

        if(thread->my_queue.current_size > MAX_QUEUE_SIZE) {
            printf("Could not add to queue: %c\n",thread->player_data.player_mark );
            return;
        }
        
        pthread_mutex_lock(lock);
        int size = (thread->my_queue.current_size);
        thread->my_queue.array[size] = new_task;
        (thread->my_queue.current_size)++;
        pthread_mutex_unlock(lock);
    }
}

void get_player_status(thread_listener_t* l,char buffer[BUFFER_SIZE], int read_bytes) {

    // message format:
    // (char) message type (int) our_player_status
    int player_status_s = 0;
    memcpy(&player_status_s, buffer + sizeof(char),sizeof(int));

    player_status status = player_status_s;

    //printf("%c received status: %d\n", l->player_data.player_mark, player_status_s);
    
    l->player_data.status = status;
    thread_task_t new_task; 
    new_task.type = send_player_status_task;
    new_task.player_mark = l->player_data.player_mark;
    new_task.that_thread_lock = &(l->my_lock);
    new_task.data = (void *) status;
    send_to_all_besides_me(l->handler,l->my_player_index,new_task);
}

void message_with_lines_to_enemy(thread_listener_t* l, char buffer[BUFFER_SIZE], int read_bytes) {
    // message format:
    // (char) message type (char)receiver_mark (char)sender_mark (int)lines_num 
    char receiver_mark = buffer[sizeof(char)];
    char sender_mark = buffer[2*sizeof(char)];
    int lines_num;
    memcpy(&lines_num,buffer + 3* sizeof(char),sizeof(int));
    if(sender_mark != l->player_data.player_mark) {
        printf("Got wrong line sender: S: %c != M: %c \n", sender_mark, l->player_data.player_mark);
    }
    printf("Lines received: R: %c, S: %c, l: %d \n",receiver_mark,sender_mark,lines_num);

    thread_task_t new_task;
    new_task.type = send_lines_task;
    new_task.player_mark = sender_mark;
    new_task.that_thread_lock = &(l->my_lock);
    new_task.data = (void *) lines_num;
    send_to_one(l->handler,abs('A'-receiver_mark), new_task);
}

void update_score(thread_listener_t* l, char buffer[BUFFER_SIZE], int read_bytes) {
    // message format:
    // (char) message type (char)player_mark (int)score (float)game_stage (int)lines_scored

    //getting new score
    score_t new_score;
    memcpy(&new_score,buffer + 2*sizeof(char),sizeof(score_t));
    printf("got score: score %d, state %f, lines %d\n",
        new_score.current_score,new_score.game_state,new_score.lines_scored);
    char player_mark = buffer[sizeof(char)];
    if(player_mark != l->player_data.player_mark) {
        printf("got wrong player: got %c <-> should be %c \n",player_mark,l->player_data.player_mark);
    }

    //updating my own score
    pthread_mutex_lock(&(l->my_lock));
    l->player_data.score = new_score;
    pthread_mutex_unlock(&(l->my_lock));

    //creating task for others
    thread_task_t new_task;
    new_task.type = send_score_task;
    new_task.player_mark = l->player_data.player_mark;
    new_task.that_thread_lock = &(l->my_lock);
    new_task.data = &(l->player_data.score);
    send_to_all_besides_me(l->handler,l->my_player_index,new_task);
}

void update_board(thread_listener_t* l, char buffer[BUFFER_SIZE]) {
    // updating local board
    pthread_mutex_lock(&(l->my_lock));
    for(int y = 0; y<BOARD_SIZE_Y; y++) {
        memcpy(l->player_data.board[y],buffer + 1 + y *BOARD_SIZE_X, BOARD_SIZE_X);
    }
    pthread_mutex_unlock(&(l->my_lock));
    
    thread_task_t new_task;
    new_task.type = send_updated_board_task;
    new_task.data = l->player_data.board;
    new_task.player_mark = l->player_data.player_mark;
    new_task.that_thread_lock = &(l->my_lock);

    send_to_all_besides_me(l->handler,l->my_player_index,new_task);

    // for(int y = 0; y<BOARD_SIZE_Y; y++) {
    //     for(int x = 0; x<BOARD_SIZE_X; x++) {
    //         printf("%c", l->player_data.board[y][x]);
    //     }
    //     printf("\n");
    // }
}

void manage_player_messages(thread_listener_t* l) {
    char buffer[BUFFER_SIZE] = {0};
    int bytes_read = read(l->client_fd,buffer, BUFFER_SIZE);
    if(bytes_read <= 0)
        return;
    
    
    switch (buffer[0])
    {
    case update_board_m:
        update_board(l,buffer);   
        //printf("%c board updated!\n", l->player_data.player_mark);
        break;
    case update_score_m:
        update_score(l,buffer,bytes_read);
        //printf("%c score updated!\n", l->player_data.player_mark);
        break;
    case send_lines_to_enemy_m:
        message_with_lines_to_enemy(l, buffer,bytes_read);
        break;
    case player_status_m:
        get_player_status(l,buffer,bytes_read);
    default:
        printf("got massage but its wrong\n");
        break;
    }
}

void send_player_status(thread_listener_t* l, thread_task_t task) { 
    // this task don't require reading from memory
    // message format:
    // (char)message_type (char)sender_mark (int)player_status;
    char buffer[BUFFER_SIZE] = {0};
    buffer[0] = player_status_m;
    buffer[1] = task.player_mark;

    int player_status = (int) task.data;
    memcpy(buffer + 2*sizeof(char),&player_status,sizeof(int));

    send(l->client_fd,buffer,2*sizeof(char) + sizeof(int),0);
    printf("send player status: %c --> %c : %d\n", task.player_mark, l->player_data.player_mark, task.data);
}

void send_lines(thread_listener_t* l, thread_task_t task) {
    // this task don't require reading from memory
    // message format:
    // (char)message_type (char)sender_mark (int)lines_num;
    
    char buffer[BUFFER_SIZE] = {0};

    buffer[0] = send_lines_to_enemy_m;
    buffer[sizeof(char)] = task.player_mark;
    int lines = (int)task.data;
    memcpy(buffer + 2*sizeof(char),&lines,sizeof(int));
    
    send(l->client_fd, buffer, 2*sizeof(char)+sizeof(int), 0);
    printf("send lines to enemy: %c --> %c : %d\n", task.player_mark, l->player_data.player_mark, task.data);
}

void send_board(thread_listener_t*l, thread_task_t task) {

    char** board =(char**) task.data;

    // sending players mark and updated board
    // message format:
    // (char) message_type (char) player_mark char board[20][10] 
    
    char buffer[BUFFER_SIZE] = {0};
    pthread_mutex_lock(task.that_thread_lock);
    for(int y = 0; y < BOARD_SIZE_Y ; y++) {
        memcpy(buffer +2 * sizeof(char) + y * BOARD_SIZE_X, board[y], BOARD_SIZE_X);
    }
    pthread_mutex_unlock(task.that_thread_lock);

    buffer[0] = update_board_m;
    buffer[1] = task.player_mark;
    send(l->client_fd,buffer,BOARD_SIZE_X * BOARD_SIZE_Y + 2, 0);
    
    printf("Board sent: %c --> %c \n", l->player_data.player_mark, task.player_mark);
}

void send_score(thread_listener_t* l, thread_task_t task) {
    score_t* score =(score_t*) task.data;

    // copying score:
    score_t score_copy;
    pthread_mutex_lock(task.that_thread_lock);
    memcpy(&score_copy, score, sizeof(score_t));
    pthread_mutex_unlock(task.that_thread_lock);
    //printf("sending score: score %d, state %f, lines %d\n",
        //score_copy.current_score,score_copy.game_state,score_copy.lines_scored);
    char buffer[BUFFER_SIZE];
    //message format:
    //(char)message_type (char)player_mark (score_t)score_copy
    int message_length  = 2*sizeof(char) + sizeof(score_t);
    memcpy(buffer + 2*sizeof(char),&score_copy, sizeof(score_t));
    buffer[0] = update_score_m;
    buffer[sizeof(char)] = task.player_mark;

    send(l->client_fd,buffer,message_length,0);
    printf("Score sent: %c --> %c \n", l->player_data.player_mark, task.player_mark); 
}

void manage_queue_tasks(thread_listener_t* l) {
    //printf("t\n");
    while(l->my_queue.current_size != 0) {
        int index = l->my_queue.current_size - 1;

        // safely removes data from queue
        pthread_mutex_lock(&(l->my_lock));
        thread_task_t task_copy = l->my_queue.array[index];
        l->my_queue.current_size--;
        pthread_mutex_unlock(&(l->my_lock));

        // decoding task
        switch (task_copy.type)
        {
        case send_score_task:
            send_score(l,task_copy);
            break;    
        case send_updated_board_task:
            send_board(l,task_copy);
            break;
        case send_lines_task:
            send_lines(l,task_copy);
            break;
        case send_player_status_task:
            send_player_status(l,task_copy);    
            break;
        default:
            printf("Wrong task sent: %c \n",l->player_data.player_mark);
            break;
        }
    }
}

int main_loop(thread_listener_t* l) {
    
    printf("%c: in main loop\n", l->player_data.player_mark);
    while(!global_game_over) {
        
        fd_set readfds;
        FD_ZERO(&readfds);
        FD_SET(l->client_fd, &readfds);
        
        struct timeval timeout = {0,100}; // 0.1 seconds timeout
        int ready = select(l->client_fd + 1, &readfds, NULL, NULL, &timeout);    

        if(ready < 0) {
            perror("select error");
            return -1;
        }else if(ready == 0) {
            // timeout occured we check the queue tasks;
            manage_queue_tasks(l);
        } else{
            // we got message from player
            manage_player_messages(l);
        }
    }
    return 0;
}

score_t* get_other_player_score(int my_index, exchange_information_handler_t* h,char player_signs[MAX_PLAYER_NUMBER]) {
    score_t* result = (score_t*) malloc(sizeof(score_t) * h->current_player_num);
    
    for(int i = 0; i < h->current_player_num; i++) {
        result[i] = h->thread_arr[i].player_data.score;
        player_signs[i] = h->thread_arr[i].player_data.player_mark;
    }
    return result;
} 

player_status* get_other_player_status(int my_index, exchange_information_handler_t* h) { 
    player_status* result = (player_status*) malloc(sizeof(player_status) * h->current_player_num);
    for(int i = 0; i < h->current_player_num; i++) {
        result[i] = h->thread_arr[i].player_data.status;
    }
    return result;
}


int send_other_players_score(thread_listener_t* l) {
    
    //those variables should not change types
    assert(sizeof(l->player_data.score.current_score) == sizeof(int));
    assert(sizeof(l->player_data.score.game_state) == sizeof(float));
    assert(sizeof(l->player_data.score.lines_scored) == sizeof(int));
    assert(sizeof(l->handler->current_player_num) == sizeof(char));
    assert(sizeof(l->player_data.status) == sizeof(player_status));
    
    // send order in binary:
    // first bytes:(int)bytes_to_read (char) player_num 
    // rest: (char)player_mark (int)current_score (float)game_state (int)lines_scored
    
    char player_signs[MAX_PLAYER_NUMBER] = {0};
    score_t* scores = get_other_player_score(l->my_player_index, l->handler, player_signs);

    player_status* statuses = get_other_player_status(l->my_player_index, l->handler);

    char buffer[BUFFER_SIZE] = {0};
    
    int start_offset = sizeof(int) + sizeof(char); // reserving 5 bytes for bytes_to_read and player_num

    for(int i = 0 ;i < l->handler->current_player_num; i++) { 
        buffer[start_offset] = player_signs[i];
        start_offset++;
        memcpy(buffer + start_offset, &scores[i], sizeof(score_t));
        start_offset += sizeof(score_t);
        memcpy(buffer + start_offset, &statuses[i],sizeof(player_status));
        start_offset += sizeof(player_status);
    }
    
    memcpy(buffer, &start_offset, sizeof(int)); // start_offset is also a size of message
    buffer[sizeof(int)] =  l->handler->current_player_num;
    send(l->client_fd, buffer,start_offset,0);
    free(scores);
    free(statuses);
}

int lobby_loop(thread_listener_t* l) {
    // to prevent potential buffer overflow critical vulnerability    
    char* buffer = (char*) malloc(sizeof(char) * BUFFER_SIZE);

    while(!start_game) {
        int bytes_read = read(l->client_fd, buffer, BUFFER_SIZE);
        if (bytes_read <= 0) {   
            printf("lobby_loop read error");    
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
    return 0;
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

    if(lobby_loop(listener) == -1) {
        return NULL;
    }
    char start_game = start_game_m;
    send(listener->client_fd,&start_game,sizeof(char),0);
    main_loop(listener);

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
    if (listen(s->server_fd, 10000) < 0) {
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

void main_game_loop(server_t* s) {
    
    printf("Main: main game loop \n");
    while(true) {

        for(int i = 0; i < s->cur_player_num; i++) {
            sleep(10);
            if(s->threads[i].player_data.status != lost_s) {
                continue;
            }
        }
        //break;
    }
    
}

int main(){
    set_nonblocking_input();
    
    server_t server;
    init_server(&server);
    init_player_data(&server);
    listen_for_connections(&server);

    main_game_loop(&server);

    close(server.server_fd);
    reset_input_mode();
}