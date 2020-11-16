package fl.wearable.autosport.sync;

public class AsyncResults {
    public int inferenceResult;
    public long curentCsvName;
    public int activityMinute;
    public int activitySecond;
    AsyncResults(int a, long b, int c, int d)
    {
        inferenceResult = a;
        curentCsvName = b;
        activityMinute = c;
        activitySecond = d;
    }
    static AsyncResults getAsyncResults(int a, long b, int c, int d)
    {
        return new AsyncResults(a, b, c, d);
    }
}
