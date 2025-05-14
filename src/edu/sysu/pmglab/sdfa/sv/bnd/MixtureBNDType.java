package edu.sysu.pmglab.sdfa.sv.bnd;

import edu.sysu.pmglab.bytecode.ByteStream;
import edu.sysu.pmglab.bytecode.Bytes;
import edu.sysu.pmglab.ccf.type.basic.BytesBox;
import edu.sysu.pmglab.ccf.type.decoder.DynamicLengthDecoder;
import edu.sysu.pmglab.ccf.type.encoder.DynamicLengthEncoder;
import edu.sysu.pmglab.container.list.List;
import edu.sysu.pmglab.easytools.Constant;
import edu.sysu.pmglab.objectpool.LinkedObjectPool;

/**
 * @author Wenjie Peng
 * @create 2024-08-29 20:10
 * @description
 */
public class MixtureBNDType extends DetailedBND {
    List<DetailedBND> types = new List<>();

    @Override
    public Bytes build() {
        ByteStream cache = new ByteStream();
        int size = types.size();
        for (int i = 0; i < size; i++) {
            cache.write(types.get(i).build());
            if (i != size - 1) {
                cache.write(Constant.COMMA);
            }
        }
        return cache.toBytes();
    }

    @Override
    public void encodeTo(ByteStream cache) {
        DynamicLengthEncoder<BytesBox> encoder = new DynamicLengthEncoder<>();
        BytesBox box = new BytesBox();
        for (DetailedBND type : types) {
            type.encodeTo(cache);
            encoder.encode(box.set(cache.toBytes()));
            cache.clear();
        }
        cache.write(encoder.flush());
    }

    @Override
    public MixtureBNDType decode(Bytes src) {
        LinkedObjectPool boxes = new LinkedObjectPool(BytesBox::new);
        DynamicLengthDecoder.INSTANCE.decodeTo(src, boxes);
        for (int i = 0; i < boxes.size(); i++) {
            BytesBox box = (BytesBox) boxes.get(i);
            types.add(DetailedBND.getInstance(box.get()).decode(box.get()));
        }
        return this;
    }

    public MixtureBNDType add(DetailedBND bndType) {
        this.types.add(bndType);
        return this;
    }

    public void clear(){
        types.clear();
    }
}
