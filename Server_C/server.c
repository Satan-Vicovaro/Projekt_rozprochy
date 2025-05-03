#include "variables.h"
#include "thread.h"

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
    
    //thread creation
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
    if (listen(s->server_fd, 14) < 0) {
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

// global variables to communicate with threads
bool start_game = false; 
bool global_game_over = false;

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