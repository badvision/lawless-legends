package jace.hardware;

import java.util.function.Function;

import jace.config.DeviceEnum;
import jace.core.Card;
import jace.core.Computer;
import jace.hardware.massStorage.CardMassStorage;

public enum Cards implements DeviceEnum<Card> {
    DiskIIDrive("Disk II Floppy Controller", CardDiskII.class, CardDiskII::new),
    MassStorage("Mass Storage", CardMassStorage.class, CardMassStorage::new),
    AppleMouse("Apple Mouse", CardAppleMouse.class, CardAppleMouse::new),
    HayesMicroModem("Hayes MicroModem", CardHayesMicromodem.class, CardHayesMicromodem::new),
    Mockingboard("Mockingboard", CardMockingboard.class, CardMockingboard::new),
    SuperSerialCard("Super Serial Card", CardSSC.class, CardSSC::new),
    RamFactor("RamFactor", CardRamFactor.class, CardRamFactor::new),
    Thunderclock("Thunderclock", CardThunderclock.class, CardThunderclock::new);

    Function<Computer, Card> factory;
    String name;
    Class<? extends Card> clazz;

    Cards(String name, Class<? extends Card> clazz, Function<Computer, Card> factory) {
        this.name = name;
        this.factory = factory;
        this.clazz = clazz;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Card create(Computer computer) {
        return factory.apply(computer);
    }

    @Override
    public boolean isInstance(Card card) {
        return card != null && clazz.isInstance(card);
    }
}