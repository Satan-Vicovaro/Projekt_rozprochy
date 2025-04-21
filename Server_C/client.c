// simple_client.c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>       // for close()
#include <arpa/inet.h>    // for inet_addr

#define SERVER_IP "127.0.0.1"
#define PORT 8080
#define BUFFER_SIZE 1024

int main() {
    int sock = 0;
    struct sockaddr_in serv_addr;
    char buffer[BUFFER_SIZE] = {0};
    char *message = "Hello from client!";

    // Create socket
    if ((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        perror("Socket creation error");
        return 1;
    }

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(PORT);

    // Convert IPv4 address from text to binary
    if (inet_pton(AF_INET, SERVER_IP, &serv_addr.sin_addr) <= 0) {
        perror("Invalid address / Address not supported");
        return 1;
    }

    // Connect to server
    if (connect(sock, (struct sockaddr*)&serv_addr, sizeof(serv_addr)) < 0) {
        perror("Connection Failed");
        return 1;
    }
    
    char output_buffer[BUFFER_SIZE] = {};
    while(1) {
        scanf("%s",output_buffer);
        // Send message
        send(sock, output_buffer, strlen(message), 0);
        printf("Message sent: %s\n", output_buffer);

        // Read server response
        int bytes_received = read(sock, buffer, BUFFER_SIZE);
        buffer[bytes_received] = '\0';  // Null-terminate received data
        printf("Server replied: %s\n", buffer);
    }

    close(sock);
    return 0;
}