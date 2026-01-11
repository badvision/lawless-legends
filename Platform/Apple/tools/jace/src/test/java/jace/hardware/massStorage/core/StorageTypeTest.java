/**
* Copyright 2024 Brendan Robert
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
**/

package jace.hardware.massStorage.core;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for StorageType enum.
 */
public class StorageTypeTest {

    @Test
    public void testSeedlingValue() {
        assertEquals(1, StorageType.SEEDLING.getValue());
    }

    @Test
    public void testSaplingValue() {
        assertEquals(2, StorageType.SAPLING.getValue());
    }

    @Test
    public void testTreeValue() {
        assertEquals(3, StorageType.TREE.getValue());
    }

    @Test
    public void testFromValueSeedling() {
        assertEquals(StorageType.SEEDLING, StorageType.fromValue(1));
    }

    @Test
    public void testFromValueSapling() {
        assertEquals(StorageType.SAPLING, StorageType.fromValue(2));
    }

    @Test
    public void testFromValueTree() {
        assertEquals(StorageType.TREE, StorageType.fromValue(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromValueInvalid0() {
        StorageType.fromValue(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromValueInvalid4() {
        StorageType.fromValue(4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromValueInvalidNegative() {
        StorageType.fromValue(-1);
    }

    @Test
    public void testSeedlingMaxFileSize() {
        assertEquals(512L, StorageType.SEEDLING.getMaxFileSize());
    }

    @Test
    public void testSaplingMaxFileSize() {
        assertEquals(128L * 1024, StorageType.SAPLING.getMaxFileSize());
    }

    @Test
    public void testTreeMaxFileSize() {
        assertEquals(256L * 256 * 512, StorageType.TREE.getMaxFileSize());
    }

    @Test
    public void testFromFileSizeSmall() {
        assertEquals(StorageType.SEEDLING, StorageType.fromFileSize(100));
        assertEquals(StorageType.SEEDLING, StorageType.fromFileSize(512));
    }

    @Test
    public void testFromFileSizeMedium() {
        assertEquals(StorageType.SAPLING, StorageType.fromFileSize(513));
        assertEquals(StorageType.SAPLING, StorageType.fromFileSize(128 * 1024));
    }

    @Test
    public void testFromFileSizeLarge() {
        assertEquals(StorageType.TREE, StorageType.fromFileSize(128 * 1024 + 1));
        assertEquals(StorageType.TREE, StorageType.fromFileSize(1024 * 1024));
    }

    @Test
    public void testFromFileSizeZero() {
        assertEquals(StorageType.SEEDLING, StorageType.fromFileSize(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromFileSizeNegative() {
        StorageType.fromFileSize(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromFileSizeTooLarge() {
        StorageType.fromFileSize(StorageType.TREE.getMaxFileSize() + 1);
    }
}
