package eu.modernmt.model;

import java.io.Serializable;
import java.util.*;

/**
 * Created by davide on 18/01/17.
 */
public class ContextVector implements Iterable<ContextVector.Entry>, Serializable {

    public static class Pair<F,S> {

        private final F first;
        private final S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public F getFirst() { return first; }
        public S getSecond() { return second; }

        @Override
        public int hashCode() { return first.hashCode() ^ second.hashCode(); }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pair)) return false;
            Pair pairo = (Pair) o;
            return this.first.equals(pairo.getFirst()) &&
                    this.second.equals(pairo.getSecond());
        }

    }

    public static class Builder {

        private final HashMap<Memory, Pair<Float,Boolean>> entries;
        private int limit = 0;

        public Builder(int initialCapacity) {
            entries = new HashMap<>(initialCapacity);
        }

        public Builder() {
            entries = new HashMap<>();
        }

        public Builder setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder add(long memory, float score) {
            return this.add(memory, score, false);
        }

        public Builder add(long memory, float score, boolean terminology) {
            this.add(new Memory(memory), score, terminology);
            return this;
        }

        public Builder add(Memory memory, float score) {
            return this.add(memory, score, false);
        }

        public Builder add(Memory memory, float score, boolean terminology) {
            if (score > 0.f)
                entries.put(memory, new Pair<Float, Boolean>(score, terminology));
            return this;
        }


        public Builder add(Entry e) {
            this.add(e.memory, e.score, e.terminology);
            return this;
        }

        public ContextVector build() {
            List<Entry> list = new ArrayList<>(this.entries.size());
            for (Map.Entry<Memory, Pair<Float,Boolean>> e : this.entries.entrySet())
                list.add(new Entry(e.getKey(), e.getValue().getFirst(), e.getValue().getSecond()));

            Collections.sort(list);
            Collections.reverse(list);

            if (limit > 0 && list.size() > limit)
                list = list.subList(0, limit);

            return new ContextVector(list.toArray(new Entry[0]));
        }
    }

    public static ContextVector fromString(String string) throws IllegalArgumentException {
        String[] elements = string.split(",");

        ContextVector.Builder builder = new ContextVector.Builder(elements.length);

        for (String element : elements) {
            String[] kv = element.split(":");

            if (kv.length != 2)
                throw new IllegalArgumentException(string);

            long memory;
            float score;
            boolean terminology = false;

            try {
                if (kv[0].charAt(0) == 't') {
                    memory = Long.parseLong(kv[0].substring(1));
                    terminology = true;
                } else {
                    memory = Long.parseLong(kv[0]);
                }
                score = Float.parseFloat(kv[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(string);
            }

            if (memory < 1)
                throw new IllegalArgumentException(string);

            if (score < 0.f || score > 1.f)
                throw new IllegalArgumentException(string);

            builder.add(memory, score, terminology);
        }

        return builder.build();
    }

    public ContextVector getTerminology(boolean terminology) {
        ContextVector.Builder builder = new ContextVector.Builder(this.size());

        for (Entry e: this.entries) {
            if (e.terminology == terminology) {
                builder.add(e);
            }
        }
        return builder.build();
    }

    public static class Entry implements Comparable<Entry>, Serializable {

        public final Memory memory;
        public final float score;
        public final boolean terminology;

        private Entry(Memory memory, float score) {
            this(memory, score, false);
        }

        private Entry(Memory memory, float score, boolean terminology) {
            this.memory = memory;
            this.score = score;
            this.terminology = terminology;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            if (Float.compare(entry.score, score) != 0) return false;
            return memory.equals(entry.memory);
        }

        @Override
        public int hashCode() {
            int result = memory.hashCode();
            result = 31 * result + (score != +0.0f ? Float.floatToIntBits(score) : 0);
            return result;
        }


        @Override
        public int compareTo(Entry o) {
            return Float.compare(score, o.score);
        }
    }

    private final Entry[] entries;

    private ContextVector(Entry[] entries) {
        this.entries = entries;
    }

    public int size() {
        return entries.length;
    }

    public boolean isEmpty() {
        return entries.length == 0;
    }

    @Override
    public Iterator<Entry> iterator() {
        return new Iterator<Entry>() {

            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < entries.length;
            }

            @Override
            public Entry next() {
                return entries[i++];
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < entries.length; i++) {
            if (i > 0)
                builder.append(',');
            if (entries[i].terminology)
                builder.append("t_");
            builder.append(entries[i].memory.getId());
            builder.append(':');
            builder.append(entries[i].score);
        }

        return builder.toString();
    }
}
