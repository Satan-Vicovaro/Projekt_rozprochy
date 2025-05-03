#ifndef THREAD_H
#define THREAD_H
#include"variables.h"

//thread communication
void send_to_one(exchange_information_handler_t* h,int receiver_index, thread_task_t new_task);
void send_to_all_besides_me(exchange_information_handler_t* h, int my_index, thread_task_t new_task);

// receive functions
void get_player_status(thread_listener_t* l,char buffer[BUFFER_SIZE], int read_bytes);
void message_with_lines_to_enemy(thread_listener_t* l, char buffer[BUFFER_SIZE], int read_bytes);
void update_score(thread_listener_t* l, char buffer[BUFFER_SIZE], int read_bytes);
void update_board(thread_listener_t* l, char buffer[BUFFER_SIZE]);
void manage_player_messages(thread_listener_t* l);

// send functions
void send_player_status(thread_listener_t* l, thread_task_t task);
void send_lines(thread_listener_t* l, thread_task_t task);
void send_board(thread_listener_t*l, thread_task_t task);
void send_score(thread_listener_t* l, thread_task_t task);

void manage_queue_tasks(thread_listener_t* l);
//game loop
int main_loop(thread_listener_t* l);

//lobby loop functions
score_t* get_other_player_score(int my_index, exchange_information_handler_t* h,char player_signs[MAX_PLAYER_NUMBER]);
player_status* get_other_player_status(int my_index, exchange_information_handler_t* h);
void send_other_players_score(thread_listener_t* l);
int lobby_loop(thread_listener_t* l);
void connect_to_lobby(thread_listener_t* l);

//main function
void* client_listener(void* arg);

#endif