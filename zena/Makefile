CC=gcc -O3
#CC=gcc -g

DEPS=-lrt -lm -lusb-1.0 

SOURCES=smartir.c color_gradient.c image_processing.c

all: zena

zena: zena.c $(DEPS)
	$(CC) -o $@ $^

.PHONY: clean

clean:
	rm -f *.o *~ zena
