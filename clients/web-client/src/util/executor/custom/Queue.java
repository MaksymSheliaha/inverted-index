package util.executor.custom;

public class Queue <T> {
    private Node<T> head;
    private Node<T> tail;
    private int size;

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(T item) {
        if(head == null) {
            head = new Node<>(item, null);
            tail = head;
        } else {
            tail.next = new Node<>(item, null);
            tail = tail.next;
        }
        size++;
    }

    public T poll() {
        if(head == null) {
            throw new IllegalStateException("Queue is empty");
        }

        T value = head.value;
        if (head == tail) {
            head = null;
            tail = null;
        } else {
            head = head.next;
        }

        size--;
        return value;
    }

    private static class Node<T> {
        T value;
        Node<T> next;

        public Node(T value, Node<T> next) {
            this.value = value;
            this.next = next;
        }
    }
}
