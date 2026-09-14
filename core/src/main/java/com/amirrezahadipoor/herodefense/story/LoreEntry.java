package com.amirrezahadipoor.herodefense.story;

/** Immutable definition of one Grove Codex entry: text plus its unlock condition. */
public final class LoreEntry {
    private final String id;
    private final int number;
    private final String title;
    private final String body;
    private final LoreTrigger trigger;
    private final String triggerParam;

    public LoreEntry(String id, int number, String title, String body, LoreTrigger trigger, String triggerParam) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.body = body;
        this.trigger = trigger;
        this.triggerParam = triggerParam;
    }

    public String id() { return id; }
    public int number() { return number; }
    public String title() { return title; }
    public String body() { return body; }
    public LoreTrigger trigger() { return trigger; }
    public String triggerParam() { return triggerParam; }
}
