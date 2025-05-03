#!binbash
gcc -Wall -fsanitize=leak -pedantic -Iinclude -c src/thread.c -o build/thread.o
ar r build/libstack.a build/thread.o 
gcc -Wall -fsanitize=leak -pedantic -Iinclude -c server.c -o build/server.o
gcc build/server.o build/libstack.a -o server
