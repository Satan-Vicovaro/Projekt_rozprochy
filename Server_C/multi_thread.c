#include <stdio.h>
#include <pthread.h>
#include <unistd.h>


// Function to be run by thread 1
void* threadFunc1(void* arg) {
    for (int i = 0; i < 10; i++) {
        printf("Thread 1: counting %d\n", i);
        sleep(2);
    }

    return NULL;
}

// Function to be run by thread 2
void* threadFunc2(void* arg) {
    for (int i = 0; i < 10; i++) {
        printf("Thread 2: counting %d\n", i);
        sleep(1);
    }
    return NULL;
}

int main() {
    pthread_t t1, t2;

    // Create threads
    pthread_create(&t1, NULL, threadFunc1, NULL);
    pthread_create(&t2, NULL, threadFunc2, NULL);

    // Wait for both threads to finish
    pthread_join(t1, NULL);
    pthread_join(t2, NULL);

    printf("Main thread: All threads have finished.\n");

    return 0;
}