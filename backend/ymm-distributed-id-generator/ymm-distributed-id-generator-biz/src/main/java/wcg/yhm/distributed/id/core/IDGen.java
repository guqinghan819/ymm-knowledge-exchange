package wcg.yhm.distributed.id.core;


import wcg.yhm.distributed.id.core.common.Result;

public interface IDGen {
    Result get(String key);
    boolean init();
}
