package org.electroncash.util;

public class Arrays
{

    private Arrays()
    {
    }

    public static List asList(Object[] a)
    {
        return new ArrayList(a);
    }

    private static class ArrayList
        extends AbstractList
    {
        private Object[] a;

        ArrayList(Object[] array)
        {
            a = array;
        }

        public int size()
        {
            return a.length;
        }

        public Object[] toArray()
        {
            Object[] tmp = new Object[a.length];

            System.arraycopy(a, 0, tmp, 0, tmp.length);

            return tmp;
        }

        public Object get(int index)
        {
            return a[index];
        }

        public Object set(int index, Object element)
        {
            Object oldValue = a[index];
            a[index] = element;
            return oldValue;
        }

        public int indexOf(Object o)
        {
            if (o == null)
            {
                for (int i = 0; i < a.length; i++)
                {
                    if (a[i] == null)
                    {
                        return i;
                    }
                }
            }
            else
            {
                for (int i = 0; i < a.length; i++)
                {
                    if (o.equals(a[i]))
                    {
                        return i;
                    }
                }
            }
            return -1;
        }

        public boolean contains(Object o)
        {
            return indexOf(o) != -1;
        }
    }
    public static byte[] reverse(byte[] arr) {
    	int length = arr.length;
    	byte[] newArray = new byte[length];
    	for(int i=0;i < length;i++) {
    		newArray[i] = arr[length-(i+1)];
    	}
    	return newArray;
    }
    
    public static byte[] slice(byte[] current_chunk,int start, int last) {
    	byte[] newArray = new byte[last-start];
    	for(int i=start; i<last ;i++) {
    		newArray[i-start] =  current_chunk[i];
    	}
    	return newArray;
    }
    
    public static void sort(int[] arr) {
    	sort(arr ,0,arr.length);
    }
    public static void sort(int x[], int off, int len) {
        if (len < 7) {
            for (int i=off; i<len+off; i++)
                for (int j=i; j>off && x[j-1]>x[j]; j--)
                    swap(x, j, j-1);
            return;
        }

        int m = off + (len >> 1);       
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        
                int s = len/8;
                l = med3(x, l,     l+s, l+2*s);
                m = med3(x, m-s,   m,   m+s);
                n = med3(x, n-2*s, n-s, n);
            }
            m = med3(x, l, m, n); 
        }
        long v = x[m];

        int a = off, b = a, c = off + len - 1, d = c;
        while(true) {
            while (b <= c && x[b] <= v) {
                if (x[b] == v)
                    swap(x, a++, b);
                b++;
            }
            while (c >= b && x[c] >= v) {
                if (x[c] == v)
                    swap(x, c, d--);
                c--;
            }
            if (b > c)
                break;
            swap(x, b++, c--);
        }

        int s, n = off + len;
        s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
        s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

        if ((s = b-a) > 1)
            sort(x, off, s);
        if ((s = d-c) > 1)
            sort(x, n-s, s);
    }
    
    private static void swap(int x[], int a, int b) {
        int t = x[a];
        x[a] = x[b];
        x[b] = t;
    }
    private static void vecswap(int x[], int a, int b, int n) {
        for (int i=0; i<n; i++, a++, b++)
            swap(x, a, b);
    }

    private static int med3(int x[], int a, int b, int c) {
        return (x[a] < x[b] ?
                (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
                (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }
}


