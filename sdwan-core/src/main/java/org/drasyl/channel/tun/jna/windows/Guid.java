/* Copyright (c) 2010 Daniel Doubrovkine, All Rights Reserved
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */
package org.drasyl.channel.tun.jna.windows;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Ported from Guid.h. Microsoft Windows SDK 6.0A.
 *
 * @author dblock[at]dblock.org
 */
public interface Guid {
    /**
     * The Class GUID.
     *
     * @author Tobias Wolf, wolf.tobias@gmx.net
     */
    @SuppressWarnings({ "java:S109", "java:S116", "java:S864", "java:S1104" })
    @FieldOrder({ "Data1", "Data2", "Data3", "Data4" })
    class GUID extends Structure {
        /**
         * The Data1.
         */
        public int Data1;
        /**
         * The Data2.
         */
        public short Data2;
        /**
         * The Data3.
         */
        public short Data3;
        /**
         * The Data4.
         */
        public byte[] Data4 = new byte[8];

        /**
         * Instantiates a new guid.
         */
        public GUID() {
            super();
        }

        /**
         * Instantiates a new guid.
         *
         * @param guid the guid
         */
        public GUID(final GUID guid) {
            this.Data1 = guid.Data1;
            this.Data2 = guid.Data2;
            this.Data3 = guid.Data3;
            this.Data4 = guid.Data4;

            this.writeFieldsToMemory();
        }

        /**
         * Instantiates a new guid.
         *
         * @param data the data
         */
        public GUID(final byte[] data) {
            this(fromBinary(data));
        }

        @Override
        public boolean equals(final Object o) {
            if (o == null) {
                return false;
            }
            if (this == o) {
                return true;
            }
            if (getClass() != o.getClass()) {
                return false;
            }

            final GUID other = (GUID) o;
            return (this.Data1 == other.Data1)
                    && (this.Data2 == other.Data2)
                    && (this.Data3 == other.Data3)
                    && Arrays.equals(this.Data4, other.Data4);
        }

        @Override
        public int hashCode() {
            return this.Data1 + this.Data2 & 0xFFFF + this.Data3 & 0xFFFF + Arrays.hashCode(this.Data4);
        }

        /**
         * From binary.
         *
         * @param data the data
         * @return the guid
         */
        public static GUID fromBinary(final byte[] data) {
            if (data.length != 16) {
                throw new IllegalArgumentException("Invalid data length: "
                        + data.length);
            }

            final GUID newGuid = new GUID();
            long data1Temp = data[0] & 0xff;
            data1Temp <<= 8;
            data1Temp |= data[1] & 0xff;
            data1Temp <<= 8;
            data1Temp |= data[2] & 0xff;
            data1Temp <<= 8;
            data1Temp |= data[3] & 0xff;
            newGuid.Data1 = (int) data1Temp;

            int data2Temp = data[4] & 0xff;
            data2Temp <<= 8;
            data2Temp |= data[5] & 0xff;
            newGuid.Data2 = (short) data2Temp;

            int data3Temp = data[6] & 0xff;
            data3Temp <<= 8;
            data3Temp |= data[7] & 0xff;
            newGuid.Data3 = (short) data3Temp;

            newGuid.Data4[0] = data[8];
            newGuid.Data4[1] = data[9];
            newGuid.Data4[2] = data[10];
            newGuid.Data4[3] = data[11];
            newGuid.Data4[4] = data[12];
            newGuid.Data4[5] = data[13];
            newGuid.Data4[6] = data[14];
            newGuid.Data4[7] = data[15];

            newGuid.writeFieldsToMemory();

            return newGuid;
        }

        /**
         * Generates a new guid. Code taken from the standard jdk implementation (see UUID class).
         *
         * @return the guid
         */
        public static GUID newGuid() {
            final SecureRandom ng = new SecureRandom();
            final byte[] randomBytes = new byte[16];

            ng.nextBytes(randomBytes);
            randomBytes[6] &= 0x0f;
            randomBytes[6] |= 0x40;
            randomBytes[8] &= 0x3f;
            randomBytes[8] |= 0x80;

            return new GUID(randomBytes);
        }

        /**
         * Write fields to backing memory.
         */
        protected void writeFieldsToMemory() {
            for (final String name : getFieldOrder()) {
                this.writeField(name);
            }
        }
    }
}
