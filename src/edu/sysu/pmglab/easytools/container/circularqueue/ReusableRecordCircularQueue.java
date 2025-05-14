package edu.sysu.pmglab.easytools.container.circularqueue;

import edu.sysu.pmglab.ccf.record.IRecord;
import edu.sysu.pmglab.container.list.List;

/**
 * @author Wenjie Peng
 * @create 2024-09-20 00:33
 * @description
 */
public class ReusableRecordCircularQueue extends ReusableCircularQueue<IRecord> {

    @Override
    public void enqueue(IRecord element) {
        if (element == null) {
            throw new NullPointerException("Element cannot be null");
        }
        if (isFull()) {
            expandCapacity();
        }
        IRecord record = (IRecord) elements[tail];
        if (record == null){
            elements[tail] = element.clone();
        }else {
            record.setFrom(element,false);
        }
        tail = (tail + 1) % elements.length;
        size++;
    }

    @Override
    public IRecord dequeue() {
        if (isEmpty()) {
            return null;
        }
        IRecord element = (IRecord) elements[head];
        element.clear();
        head = (head + 1) % elements.length;
        size--;
        reuseElement(element); // Call to method that should handle reusing element
        return element;
    }

    @Override
    protected void reuseElement(IRecord element) {
        element.clear();
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            fastGet(i).clear();
        }
        head = 0;
        tail = 0;
        size = 0;
    }

    public List<IRecord> subList(int startIndex, int toIndex){
        int size = toIndex - startIndex;
        List<IRecord> records = new List<>(size);
        for (int i = 0; i < size; i++) {
            records.set(i, (IRecord) elements[(head + i) % elements.length]);
        }
        return records;
    }
}
