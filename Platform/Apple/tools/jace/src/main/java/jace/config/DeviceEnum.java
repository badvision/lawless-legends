package jace.config;

import jace.core.Computer;

public interface DeviceEnum<T extends Reconfigurable> {
    public String getName();
    public T create(Computer computer);
    public boolean isInstance(T t);
}