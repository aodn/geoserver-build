package au.org.emii.ncdfgenerator;

public interface IAddValue {
    void prepare();

    // change name to put(), or append? and class to IBufferAddValue
    void addValueToBuffer(Object value);
}


