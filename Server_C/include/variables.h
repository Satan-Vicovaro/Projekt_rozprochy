#ifndef VARIABLES_H
#define VARIABLES_H

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

extern bool start_game; 
extern bool global_game_over;

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


#endif