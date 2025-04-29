// simple_server.c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>      // for close()
#include <arpa/inet.h>   // for socket functions
#include <pthread.h>    // threads
#include <netdb.h>
#include <sys/select.h>


#define PORT 8080
#define BUFFER_SIZE 1024

#define BOARD_SIZE_X 10
#define BOARD_SIZE_Y 20
#define MAX_PLAYER_COUNT 16

#define MAX_QUEUE_SIZE 32 

typedef enum bool {
    true = 2137,
    false = 0
}bool;

typedef enum message_type {
    join_lobby = (char) 9,
    update_board = (char) 10,
    update_score = (char) 11,
    get_lines_from_player = (char) 12,
    send_lines_to_player  = (char) 13,
} message_type;

typedef struct player_score_t {
    int score;
    int lines_cleared;
    float game_stage;
}player_score_t;

typedef struct player_data_t{
    char player;
    char **board;
    bool is_ready;
    player_score_t score;
}player_data_t;

typedef struct task_queue_t {
    thread_task_t array[MAX_QUEUE_SIZE];
    int current_size;
} task_queue_t;

typedef struct thread_task_t {
    message_type task; 
    player_data_t* data;
} thread_task_t;

typedef struct client_listener_t client_listener_t;

typedef struct client_listener_t { 
    player_data_t player;
    pthread_mutex_t *lock;    
    task_queue_t queue;
    
    task_manager_t* task_manager_ref;
    client_listener_t* thread_arr_ref;
    int *cur_player_num;

    int client_fd;
    int my_index;
}client_listener_t;


typedef struct server_t {
    
    //shared data
    client_listener_t *thread_array;

    pthread_mutex_t lock;
    pthread_t client_handlers[MAX_PLAYER_COUNT];
   
    thread_task_t *thread_task_array;
    
    //server data
    int player_num;
    int server_fd;
    struct sockaddr_in address;
    int addrlen;    
}server_t;

bool start_game = false;

typedef struct task_manager_t{
    task_queue_t *task_queue_array;
    int curent_size;
    int max_size;
    pthread_mutex_t *queue_lock;
}task_manager_t;

thread_task_t pop_task_from_queue(task_queue_t* queue, pthread_mutex_t* queue_lock ) {
    thread_task_t return_value;
    if(queue->current_size <0) {
        printf("queue is empty\n");
        return_value.data = NULL;
        return return_value;
    }

    pthread_mutex_lock(queue_lock);
    return_value = queue->array[queue->current_size - 1];
    queue->current_size--;
    pthread_mutex_unlock(queue_lock);

    return return_value;
}

int add_task_to_queue(thread_task_t task, task_queue_t* queue, pthread_mutex_t* queue_lock) {
    //critical section
    pthread_mutex_lock(queue_lock);
    if(queue->current_size >= MAX_QUEUE_SIZE) {
        return 1;
    }
    queue->array[queue->current_size] = task;
    queue->current_size++;
    pthread_mutex_unlock(queue_lock);
}

void send_task_to_all(int sender_index, thread_task_t task, task_manager_t* t_m) {
    for(int i=0; i < t_m->max_size; i++) {
        if (i == sender_index) {
         continue;
        }
        if(add_task_to_queue(task, &(t_m->task_queue_array[i]), t_m->queue_lock)) {
            printf("could not add task to queue\n");
        }
    }
}

void send_task_to_one(int sender_index, int receiver_index, thread_task_t task,task_manager_t* t_m) {
    if(receiver_index < 0 || receiver_index >= t_m->curent_size) {
        printf("wrong receiver index\n");
        return;        
    }
    if(sender_index < 0 || sender_index >= t_m->curent_size) {
        printf("wrong sender index\n");
        return;        
    }
    
    if(add_task_to_queue(task, &(t_m->task_queue_array[receiver_index]), t_m->queue_lock)) {
        printf("could not add task to queue\n");
    }
}

void add_queue_to_task_manager(task_manager_t* t_m,task_queue_t* thread_queue ) {
    
    if(t_m->curent_size >= t_m->max_size) {
        printf("task manager queue is full\n");
        return;
    }
    t_m->task_queue_array[t_m->curent_size - 1] = *thread_queue;
    t_m->curent_size++;
}

void init_task_manager(task_manager_t* t_m) {
    t_m->max_size = MAX_PLAYER_COUNT;
    t_m->curent_size = 0;
    t_m->task_queue_array = (task_queue_t*)malloc(sizeof(task_queue_t)*MAX_PLAYER_COUNT);
    pthread_mutex_init(t_m->queue_lock,NULL);
}


void handle_server_tasks(client_listener_t *l) {

   //do {
   //    thread_task_t task = pop_task_from_queue();
   //} while();
}

void handle_player_tasks(client_listener_t *l) {
    char buffer[BUFFER_SIZE] = "\0";
    int bytes_read = read(l->client_fd, buffer, BUFFER_SIZE);
    if (bytes_read <= 0) {
        return;
    }
    message_type message_type = buffer[0];

    switch (message_type)
    {
    case update_board:
        printf("Todo: update_board\n");
        
        //updating board
        int index = 1;
        for(int y = 0; y < BOARD_SIZE_Y; y++) {
            for(int x = 0; x < BOARD_SIZE_X; x++) {
                l->player.board[y][x] = buffer[index];
                index++;
            }
        }
       
        // send information to other players
        thread_task_t task;
        task.task = update_board;
        task.data = &(l->player);

        /* code */
        break;
    case update_score:
        printf("Todo: update score\n");
        /* code */
        break;
    case get_lines_from_player:
        printf("Todo: get lines from player\n");
        /* code */
        break;
    default:
        printf("Got wrong message_type at player task \n");
        break;
    }
}

void main_thread_loop(client_listener_t *l) {
    
    //setting up breaking from read() funtion
    struct timeval timeout = {5,0}; //5.0 second timeout
    fd_set read_fds;
    FD_ZERO(&read_fds);
    FD_SET(l->client_fd,&read_fds); // add client)fd to select

    while (true) {        
        int ret = select(l->client_fd + 1, &read_fds,NULL,NULL,&timeout); // waits timeout's seconds for input
        
        if (ret == -1) {
            perror("select");
            return;
        } else if (ret == 0){
            printf("Timeout: looking for server tasks: %c\n", l->player.player);
            handle_server_tasks(l);
        } else {
            printf("Handling player info: %c\n", l->player.player);
            handle_player_tasks(l);
        }     
    }
}

void connect_to_lobby(client_listener_t *l) {

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

        // Send to player their data
        buffer[0]=l->player.player;
        send(l->client_fd, buffer, 1, 0);
        printf("Sent to player:%c -> %c\n",l->player.player, l->player.player);
        player_in_lobby = true;
        l->player.is_ready = true;
    };
}

void* client_listener(void* arg) {

    client_listener_t* listener = (client_listener_t*) arg;
    printf("I started working %c \n", listener->player.player);

    connect_to_lobby(listener);

    char buffer [BUFFER_SIZE] = {0};
    while(!start_game) { 
        int bytes_read = read(listener->client_fd, buffer, BUFFER_SIZE);
        if (bytes_read <= 0) {
            break;
        }
        //printf("Received: %s\n", buffer);
        if (strcmp(buffer, "exit") == 0){
            break;
        }
        if (strcmp(buffer, "ready\0") == 0) {
            printf("Player %c is ready\n", listener->player.player);
            listener->player.is_ready = true; 
            buffer[0]=1;
            send(listener->client_fd,buffer,1,0); // sends 1 if ok
            continue;
        }
        else if (strcmp(buffer,"not ready\0") == 0) {
            printf("Player %c is not ready\n",listener->player.player);
            
            listener->player.is_ready = false;
            buffer[0]=1;
            send(listener->client_fd,buffer,1,0); // sends 1 if ok
            continue;
        } else if(strcmp(buffer,"get other players\0") == 0) {
            
            client_listener_t* threads =  listener->thread_arr_ref;
            int message_length = 4 + 1; // reserving first 4 bytes for message length (java inbuffer issues) + player number 1 byte
            for (int i=0;i<*(listener->cur_player_num);i++) {
                if (i == listener->my_index) 
                    continue;
                
                
                memcpy(buffer + message_length, &threads[i].player.player, sizeof(threads[i].player.player));
                message_length += sizeof(threads[i].player.player); // should be sizeof(char)
                
                memcpy(buffer + message_length,&threads[i].player.score.game_stage, sizeof(threads[i].player.score.game_stage));
                message_length += sizeof(threads[i].player.score.game_stage); // should be sizeof(float)

                memcpy(buffer + message_length,&threads[i].player.score.score,sizeof(threads[i].player.score.score));
                message_length += sizeof(threads[i].player.score.score); // should be sizeof(int)
                
                buffer[message_length] =(char) -1; // end of player sign
                message_length++;
            }
            buffer[message_length] = '\0';
            message_length++;

            memcpy(buffer,&message_length,sizeof(int));
            buffer[4] = (char) *(listener->cur_player_num);
            
            send(listener->client_fd,buffer,BUFFER_SIZE,0);
            continue;
        }
        printf("Wrong message sent\n");
    }

    //main_thread_loop(listener);

    return NULL; 
}

void listen_for_connections(server_t* s) {
   
    printf("Waiting for players to join\n");
    int cur_player_count = 0;
    while (cur_player_count < MAX_PLAYER_COUNT) { // waiting players to connect 
        int client_fd = 0;
        
        // waiting for connection
        if ((client_fd = accept(s->server_fd, (struct sockaddr*)&(s->address),(socklen_t*)& (s->addrlen))) < 0) { 
            perror("accept failed");
            close(s->server_fd);
            exit(EXIT_FAILURE);
        }
        printf("Player accepted\n");
        
        s->thread_array[cur_player_count].client_fd = client_fd;

        pthread_create(&(s->client_handlers[cur_player_count]),NULL,client_listener, &(s->thread_array[cur_player_count]));        
        cur_player_count++;
        s->player_num = cur_player_count;
    }
}

void init_player_data_at_server(server_t* s, task_manager_t* task_manager) {

    char ***player_boards;

    //player board init;
    player_boards = (char***)malloc(sizeof(char*)*MAX_PLAYER_COUNT);
    for(int z = 0; z < MAX_PLAYER_COUNT;z++) {
        player_boards[z] = (char**) malloc(sizeof(char*)*BOARD_SIZE_Y);
        for(int y = 0; y<BOARD_SIZE_Y;y++) {
            player_boards[z][y] = (char*)malloc(sizeof(char)*BOARD_SIZE_X);
            for(int x = 0; x<BOARD_SIZE_X;x++) {
                player_boards[z][y][x] = '1' + x;
            }    
        }
    }
    
    // mutext lock
    pthread_mutex_init(&(s->lock), NULL);    
    
    // thread data array
    client_listener_t* player_listener_array = (client_listener_t*) malloc(sizeof(client_listener_t) * MAX_PLAYER_COUNT);
    for(int i =0; i<MAX_PLAYER_COUNT;i++) {
        player_listener_array[i].client_fd = -1;
        player_listener_array[i].lock = &(s->lock);
        player_listener_array[i].thread_arr_ref = player_listener_array;
        player_listener_array[i].my_index = i;
        player_listener_array[i].cur_player_num = &(s->player_num);
        player_listener_array[i].task_manager_ref = task_manager;

        add_queue_to_task_manager(task_manager, &(player_listener_array[i].queue));

        player_listener_array[i].player.board = player_boards[i]; 
        player_listener_array[i].player.player = (char)('A' + i); 
        player_listener_array[i].player.is_ready = false;

        player_listener_array[i].player.score.game_stage = 0.0;
        player_listener_array[i].player.score.lines_cleared = 0;
        player_listener_array[i].player.score.score = 0;
    }
    s->thread_array = player_listener_array;

}

void start_server(server_t* s) {
    // Create socket file descriptor
    if ((s->server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
        perror("socket failed");
        exit(EXIT_FAILURE);
    }

    // Bind to port
    s->address.sin_family = AF_INET;
    s->address.sin_addr.s_addr = INADDR_ANY; // <- listening on this port
    s->address.sin_port = htons(PORT);
    
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
        
}

int main() {
    server_t server;

    start_server(&server);
    
    task_manager_t task_manager;
    init_task_manager(&task_manager);
    
    init_player_data_at_server(&server, &task_manager);
    listen_for_connections(&server);
 
    // Clean up
    close(server.server_fd);

    return 0;
}
