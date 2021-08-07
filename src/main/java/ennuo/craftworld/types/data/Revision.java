package ennuo.craftworld.types.data;

public class Revision {
    public Revision(int head) { this.head = head; this.branch = 0; }
    public Revision(int head, int branch) { this.head = head; this.branch = branch; }
    
    public int head = 0;
    public int branch = 0;
}
