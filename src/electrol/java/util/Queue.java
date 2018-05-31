package electrol.java.util;

import java.util.NoSuchElementException;

public class Queue
{
    protected Object queue[] ;
    protected int front, rear, size, len;
 
    public Queue(int n) 
    {
        size = n;
        len = 0;
        queue = new Object[size];
        front = -1;
        rear = -1;
    }    
    public boolean isEmpty() 
    {
        return front == -1;
    }    
    public boolean isFull() 
    {
        return front==0 && rear == size -1 ;
    }    
    public int getSize()
    {
        return len ;
    }    
    public Object peek() 
    {
        if (isEmpty())
           throw new NoSuchElementException("Underflow Exception");
        return queue[front];
    }    
    public void insert(Object i) 
    {
        if (rear == -1) 
        {
            front = 0;
            rear = 0;
            queue[rear] = i;
        }
        else if (rear + 1 >= size)
            throw new IndexOutOfBoundsException("Overflow Exception");
        else if ( rear + 1 < size)
            queue[++rear] = i;    
        len++ ;    
    }    
    public Object remove() 
    {
        if (isEmpty())
           throw new NoSuchElementException("Underflow Exception");
        else 
        {
            len-- ;
            Object ele = queue[front];
            if ( front == rear) 
            {
                front = -1;
                rear = -1;
            }
            else
                front++;                
            return ele;
        }        
    }
    public void display()
    {
        System.out.print("\nQueue = ");
        if (len == 0)
        {
            System.out.print("Empty\n");
            return ;
        }
        for (int i = front; i <= rear; i++)
            System.out.print(queue[i]+" ");
        System.out.println();        
    }
}
