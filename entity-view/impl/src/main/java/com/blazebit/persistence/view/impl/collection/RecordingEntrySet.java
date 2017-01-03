/*
 * Copyright 2014 - 2017 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blazebit.persistence.view.impl.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class RecordingEntrySet<C extends Map<K, V>, K, V> implements Set<Map.Entry<K, V>> {

    protected final Set<Map.Entry<K, V>> delegate;
    protected final RecordingMap<C, K, V> recordingMap;

    public RecordingEntrySet(Set<Map.Entry<K, V>> delegate, RecordingMap<C, K, V> recordingMap) {
        this.delegate = delegate;
        this.recordingMap = recordingMap;
    }

    @Override
    public boolean add(Map.Entry<K, V> e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends Map.Entry<K, V>> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        recordingMap.actions.add(new MapRemoveEntryAction<C, K, V>(o));
        return delegate.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        recordingMap.actions.add(new MapRemoveAllEntriesAction<C, K, V>(c));
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        recordingMap.actions.add(new MapRetainAllEntriesAction<C, K, V>(c));
        return delegate.retainAll(c);
    }

    @Override
    public void clear() {
        recordingMap.actions.add(new MapClearAction<C, K, V>());
        delegate.clear();
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return new RecordingIterator<Iterator<Map.Entry<K, V>>, Map.Entry<K, V>>(delegate.iterator());
    }
    
    /**************
     * Read-only
     *************/

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

}
