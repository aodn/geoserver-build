package au.org.emii.ncdfgenerator;

class AttributeValue {
    private final int pos;
    private final Object value;

    AttributeValue(int pos, Object value) {
        this.pos = pos;
        this.value = value;
    }

    int getPosition() {
        return pos;
    }

    Object getValue() {
        return value;
    }
}


