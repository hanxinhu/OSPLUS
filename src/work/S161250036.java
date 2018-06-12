package work;

import bottom.BottomMonitor;
import bottom.BottomService;
import bottom.Task;
import main.Schedule;

import java.io.IOException;

/**
 * 注意：请将此类名改为 S+你的学号   eg: S161250001
 * 提交时只用提交此类和说明文档
 * <p>
 * 在实现过程中不得声明新的存储空间（不得使用new关键字，反射和java集合类）
 * 所有新声明的类变量必须为final类型
 * 不得创建新的辅助类
 * <p>
 * 可以生成局部变量
 * 可以实现新的私有函数
 * <p>
 * 可用接口说明:
 * <p>
 * 获得当前的时间片
 * int getTimeTick()
 * <p>
 * 获得cpu数目
 * int getCpuNumber()
 * <p>
 * 对自由内存的读操作  offset 为索引偏移量， 返回位置为offset中存储的byte值
 * byte readFreeMemory(int offset)
 * <p>
 * 对自由内存的写操作  offset 为索引偏移量， 将x写入位置为offset的内存中
 * void writeFreeMemory(int offset, byte x)
 */
public class S161250036 extends Schedule {
    private static final int resourceBitBegin = 0;
    /**
     * 记录栈底的储存位置
     */
    private static final int cleanLengthBegin = 128;
    private static final int cleanResourceBegin = cleanLengthBegin + 2;
    private static final int ebpBeginner = cleanResourceBegin + 128;
    /**
     * 记录栈顶的储存位置
     */
    private static final int espBeginner = ebpBeginner + 4;
    /**
     * EBP 的初始值 从这里开始 即PCB的开始点
     */
    private static final int EBP = espBeginner + 4;
    /**
     * ESP 的最小值
     */
    private static final int ESP = espBeginner + 4;

    private static final int pcb_tidBeginner = 0;
    private static final int pcb_runningBeginner = 4;

    private static final int pcb_leftTimeBeginner = 5;
    private static final int pcb_rsLengthBeginner = 7;
    private static final int pcb_resourceBeginner = 8;
    private static final int length = 20;

    @Override
    public void ProcessSchedule(Task[] arrivedTask, int[] cpuOperate) {

        int esp = recordTasks(arrivedTask);

        int cpuCount = 0;
        int cpuNumber = getCpuNumber();
        for (int i = EBP; i < esp && cpuCount < cpuNumber; ) {
            int tid = readShort(i);
            if (tid == 0) {
                i += 2;
                while (readFreeMemory(i) == 0 && i < esp) {
                    i++;
                }
                continue;
            }
            int isRunning = readFreeMemory(i + pcb_runningBeginner);
            int resourceLength = readFreeMemory(i + pcb_rsLengthBeginner) + 1;
            int leftTime = readShort(i + pcb_leftTimeBeginner);
            if (isRunning > 0) {
                cpuOperate[cpuCount++] = tid;
                leftTime--;
                writeShort(i + pcb_leftTimeBeginner, leftTime);
                if (leftTime == 0) {
                    writeFreeMemory(i + pcb_runningBeginner, (byte) 0);
                    System.out.println(tid + " is finished");
                    cleanResource(i, resourceLength);
                }
            } else if (leftTime > 0) {
//                System.out.println(tid + " is trying to use:");
                if (useResource(i, resourceLength)) {
                    leftTime--;
                    cpuOperate[cpuCount++] = tid;
//                    System.out.println(cpuCount + " is running " + tid);
                    writeShort(i + pcb_leftTimeBeginner, leftTime);
                    if (leftTime == 0) {
                        writeFreeMemory(i + pcb_runningBeginner, (byte) 0);
//                        System.out.println(tid + " is finished");
                        cleanResource(i, resourceLength);
                    } else
                        writeFreeMemory(i + pcb_runningBeginner, (byte) 1);
                }
            }
            i += pcb_resourceBeginner + resourceLength;
        }
        clean();


    }


    /**
     * 记录到达的tasks信息 并返回 task
     *
     * @param tasks
     * @return
     */
    private int recordTasks(Task[] tasks) {
        if (tasks==null && tasks.length == 0)
            return readInteger(espBeginner);
        int esp = readInteger(espBeginner);
        if (esp < ESP)
            esp = ESP;
        for (int i = 0; i < tasks.length; i++) {
            writeShort(esp + pcb_tidBeginner, tasks[i].tid);
            writeFreeMemory(esp + pcb_runningBeginner, (byte) 0);
            writeShort(esp + pcb_leftTimeBeginner, tasks[i].cpuTime);
            writeFreeMemory(esp + pcb_rsLengthBeginner, (byte) (tasks[i].resource.length - 1));
            for (int j = 0; j < tasks[i].resource.length; j++) {
                writeFreeMemory(esp + pcb_resourceBeginner + j, (byte) (tasks[i].resource[j] - 1));
            }
            esp += pcb_resourceBeginner + tasks[i].resource.length;
        }
        writeInteger(espBeginner, esp);
        return esp;
    }

    private boolean useResource(int taskBeginner, int resourceLength) {
//        System.out.print("test : ");
        for (int i = 0; i < resourceLength; i++) {
            byte temple = readFreeMemory(taskBeginner + pcb_resourceBeginner + i);
//            System.out.print(temple + " ");
            if (readFreeMemory(temple + resourceBitBegin) != 0) {
//                System.out.println();
                return false;
            }
        }
//        System.out.println();
//        System.out.print("use :");
        for (int i = 0; i < resourceLength; i++) {
            byte temple = readFreeMemory(taskBeginner + pcb_resourceBeginner + i);
            writeFreeMemory(temple + resourceBitBegin, (byte) 1);
//            System.out.print(temple + " ");
        }
//        System.out.println();
        return true;
    }

    private void cleanResource(int taskBeginner, int resourceLength) {
        int cleanLength =   readShort(cleanLengthBegin);
//        System.out.print("is going to clean :");
        for (int i = 0; i < resourceLength; i++) {
            byte temple = readFreeMemory(taskBeginner + pcb_resourceBeginner + i);
            writeFreeMemory(cleanResourceBegin+cleanLength+i,temple);

//            System.out.print(temple +" ");
        }
        cleanLength += resourceLength;
        writeShort(cleanLengthBegin,cleanLength);
//        System.out.println("clean length is " +cleanLength);
    }
    private void clean(){
        int cleanLength =   readShort(cleanLengthBegin);
        if (cleanLength > 0) {
//            System.out.print("clean :" );
            for (int i = 0; i < cleanLength; i++) {
                byte temple = readFreeMemory(cleanResourceBegin + i);
                writeFreeMemory(temple + resourceBitBegin, (byte) 0);
//                System.out.print(" " + temple);
            }
            writeShort(cleanLengthBegin,0);
//            System.out.println();
        }

    }

    /**
     * 读取两个字节
     *
     * @param beginIndex
     * @return
     */
    private int readShort(int beginIndex) {
        int ans = 0;
        ans += (readFreeMemory(beginIndex) & 0xff) << 8;
        ans += (readFreeMemory(beginIndex + 1) & 0xff);
        return ans;
    }

    /**
     * 写入两个字节的
     *
     * @param beginIndex
     * @param value
     */
    private void writeShort(int beginIndex, int value) {
        writeFreeMemory(beginIndex + 1, (byte) ((value & 0x000000ff)));
        writeFreeMemory(beginIndex + 0, (byte) ((value & 0x0000ff00) >> 8));
    }

    /**
     * 向自由内存中 读一个int型整数
     *
     * @param beginIndex
     * @return
     */
    private int readInteger(int beginIndex) {
        int ans = 0;
        ans += (readFreeMemory(beginIndex) & 0xff) << 24;
        ans += (readFreeMemory(beginIndex + 1) & 0xff) << 16;
        ans += (readFreeMemory(beginIndex + 2) & 0xff) << 8;
        ans += (readFreeMemory(beginIndex + 3) & 0xff);
        return ans;
    }

    /**
     * 向自由内存中写一个int型整数
     *
     * @param beginIndex
     * @param value
     */
    private void writeInteger(int beginIndex, int value) {
        writeFreeMemory(beginIndex + 3, (byte) ((value & 0x000000ff)));
        writeFreeMemory(beginIndex + 2, (byte) ((value & 0x0000ff00) >> 8));
        writeFreeMemory(beginIndex + 1, (byte) ((value & 0x00ff0000) >> 16));
        writeFreeMemory(beginIndex, (byte) ((value & 0xff000000) >> 24));
    }

    /**
     * 执行主函数 用于debug
     * 里面的内容可随意修改
     * 你可以在这里进行对自己的策略进行测试，如果不喜欢这种测试方式，可以直接删除main函数
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // 定义cpu的数量
        int cpuNumber = 2;
        // 定义测试文件
        String filename = "src/testFile/textSample.txt";
        BottomMonitor bottomMonitor = new BottomMonitor(filename, cpuNumber);
        BottomService bottomService = new BottomService(bottomMonitor);
        Schedule schedule = new S161250036();
        schedule.setBottomService(bottomService);

        //外部调用实现类
        for (int i = 0; i < 500; i++) {
            Task[] tasks = bottomMonitor.getTaskArrived();
            int[] cpuOperate = new int[cpuNumber];

            // 结果返回给cpuOperate
            schedule.ProcessSchedule(tasks, cpuOperate);

            try {
                bottomService.runCpu(cpuOperate);
            } catch (Exception e) {
                System.out.println("Fail: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            bottomMonitor.increment();
        }

        //打印统计结果
        bottomMonitor.printStatistics();
        System.out.println();

        //打印任务队列
        bottomMonitor.printTaskArrayLog();
        System.out.println();

        //打印cpu日志
        bottomMonitor.printCpuLog();


        if (!bottomMonitor.isAllTaskFinish()) {
            System.out.println(" Fail: At least one task has not been completed! ");
        }
    }

}
