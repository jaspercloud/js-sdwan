package io.jasercloud.sdwan;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.DefaultAddressedEnvelope;

import java.net.InetSocketAddress;

public class Envelope<T> {

    private InetSocketAddress sender;
    private InetSocketAddress recipient;
    private T message;
    private boolean reSend;

    public InetSocketAddress sender() {
        return sender;
    }

    public InetSocketAddress recipient() {
        return recipient;
    }

    public T message() {
        return message;
    }

    public boolean reSend() {
        return reSend;
    }

    public Envelope() {
    }

    public Envelope(InetSocketAddress sender, InetSocketAddress recipient, T message, boolean reSend) {
        this.sender = sender;
        this.recipient = recipient;
        this.message = message;
        this.reSend = reSend;
    }

    public static class Builder<T> {

        private InetSocketAddress sender;
        private InetSocketAddress recipient;
        private T message;
        private boolean reSend = false;

        public Builder<T> sender(InetSocketAddress sender) {
            this.sender = sender;
            return this;
        }

        public Builder<T> recipient(InetSocketAddress recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder<T> message(T message) {
            this.message = message;
            return this;
        }

        public Builder<T> reSend(boolean reSend) {
            this.reSend = reSend;
            return this;
        }

        public Envelope<T> build() {
            Envelope<T> envelope = new Envelope<>(sender, recipient, message, reSend);
            return envelope;
        }

        public AddressedEnvelope<T, InetSocketAddress> toNettyEnvelope() {
            AddressedEnvelope<T, InetSocketAddress> envelope = new DefaultAddressedEnvelope<>(message, recipient, sender);
            return envelope;
        }
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
}
