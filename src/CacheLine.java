class CacheLine {
    boolean valid;
    long tag;
    byte[] data;

    CacheLine(int blockSize) {
        valid = false;
        data = new byte[blockSize];
    }
}

class CacheSet {
    CacheLine[] lines;

    CacheSet(int associativity, int blockSize) {
        lines = new CacheLine[associativity];
        for (int i = 0; i < associativity; i++) {
            lines[i] = new CacheLine(blockSize);
        }
    }
}
