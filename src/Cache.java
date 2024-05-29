class Cache {
    CacheSet[] sets;
    int setCount;
    int linesPerSet;
    int blockSize;

    Cache(int setCount, int linesPerSet, int blockSize) {
        this.setCount = setCount;
        this.linesPerSet = linesPerSet;
        this.blockSize = blockSize;
        sets = new CacheSet[setCount];
        for (int i = 0; i < setCount; i++) {
            sets[i] = new CacheSet(linesPerSet, blockSize);
        }
    }
}
