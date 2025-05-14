package edu.sysu.pmglab.easytools.wrapper;

import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.container.list.IntList;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.utils.ValueUtils;

import java.util.NoSuchElementException;

/**
 * @author Wenjie Peng
 * @create 2025-03-04 06:35
 * @description
 */
public class MemoryBytesSplitter {
    private static final ThreadLocal<MemoryBytesSplitter> POOL = ThreadLocal.withInitial(() -> new MemoryBytesSplitter(Constant.TAB));
    final Bytes container = new Bytes();
    final Bytes data = new Bytes();
    final byte separator;
    final int minLength;
    int pointer = -1;

    IntList memoryLengthList;
    IntList memoryPointerList;
    List<Bytes> vcfLineWrapper = new List<>(9);

    public MemoryBytesSplitter(byte separator) {
        this.minLength = 0;
        this.separator = separator;
    }

    public MemoryBytesSplitter(int minLength, byte separator) {
        this.minLength = ValueUtils.valueOf(minLength, 0, Integer.MAX_VALUE);
        this.separator = separator;
    }

    public static MemoryBytesSplitter getThreadInstance() {
        return POOL.get();
    }

    public MemoryBytesSplitter init(Bytes bytes) {
        this.data.reset(bytes);
        this.container.reset(bytes);
        this.pointer = 0;
        if (memoryPointerList == null) {
            memoryPointerList = new IntList();
        } else {
            memoryPointerList.clear();
        }
        if (memoryLengthList == null) {
            memoryLengthList = new IntList();
        } else {
            memoryLengthList.clear();
        }
        return this;
    }

    public MemoryBytesSplitter initVCFSplitter(Bytes bytes) {
        this.data.reset(bytes);
        this.container.reset(bytes);
        this.pointer = 0;
        if (memoryPointerList == null) {
            memoryPointerList = new IntList();
        } else {
            memoryPointerList.clear();
        }
        if (memoryLengthList == null) {
            memoryLengthList = new IntList();
        } else {
            memoryLengthList.clear();
        }
        if (vcfLineWrapper == null){
            vcfLineWrapper = new List<>(9);
        }
        if (vcfLineWrapper.isEmpty()){
            for (int i = 0; i < 9; i++) {
                vcfLineWrapper.add(new Bytes());
            }
        }
        return this;
    }
    public MemoryBytesSplitter init(byte[] src) {
        this.data.reset(src);
        this.container.reset(src);
        this.pointer = 0;
        return this;
    }

    public MemoryBytesSplitter init(byte[] src, int offset, int length) {
        this.data.reset(src, offset, length);
        this.container.reset(src, offset, length);
        this.pointer = 0;
        return this;
    }

    public boolean hasNext() {
        return this.pointer <= this.data.length();
    }

    public Bytes next() {
        for(int i = this.pointer + this.minLength; i < this.data.length(); ++i) {
            if (this.data.fastByteAt(i) == this.separator) {
                memoryPointerList.add(this.data.offset()+pointer);
                memoryLengthList.add(i-pointer);
                this.container.reset(this.data.bytes(), this.data.offset() + this.pointer, i - this.pointer);
                this.pointer = i + 1;
                return this.container;
            }
        }

        if (this.data.length() - this.pointer >= 0) {
            this.container.reset(this.data.bytes(), this.data.offset() + this.pointer, this.data.length() - this.pointer);
            memoryPointerList.add(data.offset()+pointer);
            memoryLengthList.add(data.length()-pointer);
            this.pointer = this.data.length() + 1;
            return this.container;
        } else {
            throw new NoSuchElementException();
        }
    }

    public void clear() {
        this.pointer = -1;
    }

    public Bytes fastGetInVCF(int index){
        int start = memoryPointerList.fastGet(index);
        return data.subBytes(start,memoryLengthList.fastGet(index)+start);
    }

    public void clearMemory(){
        memoryLengthList.clear();
        memoryPointerList.clear();
    }
}
