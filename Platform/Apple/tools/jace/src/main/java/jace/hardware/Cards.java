package jace.hardware;

import java.util.function.Supplier;

import jace.config.DeviceEnum;
import jace.core.Card;
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

    Supplier<Card> factory;
    String name;
    Class<? extends Card> clazz;

    Cards(String name, Class<? extends Card> clazz, Supplier<Card> factory) {
        this.name = name;
        this.factory = factory;
        this.clazz = clazz;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Card create() {
        return factory.get();
    }

    @Override
    public boolean isInstance(Card card) {
        return card != null && clazz.isInstance(card);
    }
}