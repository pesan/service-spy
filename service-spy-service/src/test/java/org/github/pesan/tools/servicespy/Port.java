package org.github.pesan.tools.servicespy;

final class Port {
    private static final Port ANY = new Port(0);
    private final int portNumber;

    private Port(int portNumber) {
        this.portNumber = portNumber;
    }

    static Port any() {
        return ANY;
    }

    static Port of(int portNumber) {
        if (portNumber < 0) {
            throw new IllegalArgumentException("expected portNumber to be >= 0");
        }
        return portNumber == 0 ? any() : new Port(portNumber);
    }

    public int portNumber() {
        return portNumber;
    }

    public String asText() {
        return String.valueOf(portNumber);
    }

    @Override
    public boolean equals(Object that) {
        return that instanceof Port && portNumber == ((Port)that).portNumber;
    }

    @Override
    public int hashCode() {
        return portNumber;
    }

    @Override
    public String toString() {
        return asText();
    }
}
