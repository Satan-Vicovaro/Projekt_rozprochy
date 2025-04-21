// simple_server.c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>      // for close()
#include <arpa/inet.h>   // for socket functions
#include <pthread.h>    // threads
#include <netdb.h>

#define PORT 8080
#define BUFFER_SIZE 1024

#define BOARD_SIZE_X 10
#define BOARD_SIZE_Y 20
#define MAX_PLAYER_COUNT 16

typedef enum bool {
    true = 2137,
    false =0
}bool;

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

typedef struct server_t {
    player_data_t player_array[MAX_PLAYER_COUNT];
    int player_num;
    int server_fd, client_fd;
    struct sockaddr_in address;
    int addrlen;    
}server_t;

typedef struct client_listener_t {
    pthread_mutex_t *lock;
    player_data_t player;    
    int client_fd;
}client_listener_t;

bool start_game = false;
void* client_listener(void* arg) {

    client_listener_t* listener = (client_listener_t*) arg;
    printf("I sarted working %c \n", listener->player.player);

    char buffer[BUFFER_SIZE] = "\0";
    bool player_in_lobby = false;
    while(!player_in_lobby) {
        int bytes_read = read(listener->client_fd, buffer, BUFFER_SIZE);
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
        buffer[0]=listener->player.player;
        send(listener->client_fd, buffer, 1, 0);
        printf("Sent to player:%c -> %c\n",listener->player.player, listener->player.player);
        player_in_lobby = true;
    };

    while(!start_game) { 
        int bytes_read = read(listener->client_fd, buffer, BUFFER_SIZE);
        if (bytes_read <= 0) {
            break;
        }
        //printf("Received: %s\n", buffer);
        if (strcmp(buffer, "exit") == 0){
            break;
        }
        if (strcmp(buffer, "ready\0")==0) {
            printf("Player %c is ready\n", listener->player.player);
            listener->player.is_ready = true; 
            buffer[0]=1;
            send(listener->client_fd,buffer,1,0); // sends 1 if ok
            continue;
        }
        else if (strcmp(buffer,"not ready\0")==0) {
            printf("Player %c is not ready\n",listener->player.player);
            
            listener->player.is_ready = false;
            buffer[0]=1;
            send(listener->client_fd,buffer,1,0); // sends 1 if ok
            continue;
        }
        printf("Wrong message sent\n");

    }

    return NULL; 
} 


int main() {
    int server_fd, client_fd;
    struct sockaddr_in address;
    int addrlen = sizeof(address);
    char buffer[BUFFER_SIZE] = {0};

    
    // Create socket file descriptor
    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
        perror("socket failed");
        exit(EXIT_FAILURE);
    }

    // Bind to port
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY; // <- listening on this port
    address.sin_port = htons(PORT);
    
    if (bind(server_fd, (struct sockaddr*) &address, sizeof(address)) < 0) {
        perror("bind failed");
        close(server_fd);
        exit(EXIT_FAILURE);
    }

    // Listen for connections
    if (listen(server_fd, 3) < 0) {
        perror("listen failed");
        close(server_fd);
        exit(EXIT_FAILURE);
    }
    printf("Server is listening on port %d...\n", PORT);

    char hostname[128];
    gethostname(hostname, sizeof(hostname));
    struct hostent *host_entry = gethostbyname(hostname);
    char *IPbuffer = inet_ntoa(*((struct in_addr*) host_entry->h_addr_list[0]));

    printf("Server IP: %s\n", IPbuffer);

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


    pthread_mutex_t lock;
    pthread_mutex_init(&lock, NULL);    
    pthread_t client_handlers[MAX_PLAYER_COUNT];

    printf("Waiting for players to join\n");
    int cur_player_count = 0;
    while (cur_player_count < MAX_PLAYER_COUNT) { // waiting players to connect 
        int client_fd = 0;
        
        // waiting for connection
        if ((client_fd = accept(server_fd, (struct sockaddr*)&address,(socklen_t*)&addrlen)) < 0) { 
            perror("accept failed");
            close(server_fd);
            exit(EXIT_FAILURE);
        }

        printf("Player accepted\n");
        // creating client handler
        client_listener_t* player_listener = (client_listener_t*)malloc(sizeof(client_listener_t));
        
        player_listener->client_fd = client_fd;
        player_listener->lock = &lock;

        player_listener->player.board = player_boards[cur_player_count];
        player_listener->player.player = (char)('A' + cur_player_count);
        player_listener->player.is_ready = false;
        
        player_listener->player.score.game_stage = 0.0;
        player_listener->player.score.lines_cleared = 0;
        player_listener->player.score.score = 0;
        
        pthread_create(&client_handlers[cur_player_count],NULL,client_listener, player_listener);        
        
        cur_player_count++;
    }
 
    // Clean up
    close(client_fd);
    close(server_fd);

    return 0;
}
