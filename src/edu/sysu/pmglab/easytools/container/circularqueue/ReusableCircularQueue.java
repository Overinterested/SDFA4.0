package edu.sysu.pmglab.easytools.container.circularqueue;

import edu.sysu.pmglab.ccf.field.FieldGroupMetas;
import edu.sysu.pmglab.ccf.field.FieldMeta;
import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.ccf.record.Record;
import edu.sysu.pmglab.ccf.type.FieldType;

import java.util.Arrays;

/**
 * @author Wenjie Peng
 * @create 2024-09-10 01:22
 * @description
 */
public abstract class ReusableCircularQueue<E> {
    protected int head;
    protected int tail;
    protected int size;
    protected Object[] elements;
    protected static final int DEFAULT_CAPACITY = 10;

    @SuppressWarnings("unchecked")
    public ReusableCircularQueue(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("Initial capacity must be positive");
        }
        elements = (E[]) new Object[initialCapacity];
        head = 0;
        tail = 0;
        size = 0;
    }

    public ReusableCircularQueue() {
        this(DEFAULT_CAPACITY);
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == elements.length;
    }

    public void enqueue(E element) {
        if (element == null) {
            throw new NullPointerException("Element cannot be null");
        }
        if (isFull()) {
            expandCapacity();
        }
        elements[tail] = element;
        tail = (tail + 1) % elements.length;
        size++;
    }

    public E dequeue() {
        if (isEmpty()) {
            return null;
        }
        E element = (E) elements[head];
        elements[head] = null; // Clear reference for garbage collection
        head = (head + 1) % elements.length;
        size--;
        reuseElement(element); // Call to method that should handle reusing element
        return element;
    }

    public void dropFirst() {
        if (isEmpty()) {
            return;
        }
        E element = (E) elements[head];
        reuseElement(element);
        head = (head + 1) % elements.length;
        size--;
    }

    protected abstract void reuseElement(E element);

    protected void expandCapacity() {
        int newCapacity = (elements.length * 3) / 2;
        E[] newElements = (E[]) new Object[newCapacity];
        for (int i = 0; i < size; i++) {
            newElements[i] = (E) elements[(head + i) % elements.length];
        }
        elements = newElements;
        head = 0;
        tail = size;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return elements.length;
    }

    public void clear() {
        Arrays.fill(elements, null);
        head = 0;
        tail = 0;
        size = 0;
    }

    public E get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
        int actualIndex = (head + index) % elements.length;  // Calculate the actual position in the circular array
        return (E) elements[actualIndex];
    }

    public E fastGet(int index) {
        int actualIndex = (head + index) % elements.length;  // Calculate the actual position in the circular array
        return (E) elements[actualIndex];
    }

    public static void main(String[] args) {
        ReusableCircularQueue<IRecord> queue = new ReusableCircularQueue<IRecord>(4) {
            @Override
            protected void reuseElement(IRecord element) {
                element.clear();
            }

            @Override
            public void enqueue(IRecord element) {
                super.enqueue(element);
            }
        };
        IRecord record = new Record(new FieldGroupMetas(FieldMeta.of("Index::index", FieldType.varInt32)));
        queue.enqueue(record.clone().set(0, 1));
        System.out.println((int) queue.fastGet(0).get(0));
        queue.enqueue(record.clone().set(0, 2));
        System.out.println((int) queue.fastGet(1).get(0));
        queue.enqueue(record.clone().set(0, 3));
        System.out.println((int) queue.fastGet(2).get(0));
        queue.enqueue(record.clone().set(0, 4));
        System.out.println((int) queue.fastGet(3).get(0));
        System.out.println(queue.isFull());
        queue.enqueue(record.clone().set(0, 5));
        System.out.println((int) queue.fastGet(4).get(0));
    }
}
