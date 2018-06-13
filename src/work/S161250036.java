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
    private static final int MEM_SIZE = 20 * 1024 - 1024;

    /**
     * 留1M资源来存 资源占用相关信息
     */
    private static final int resourceBitBegin = MEM_SIZE;
    /**
     * 记录长度
     */
    private static final int cleanResourceBegin = resourceBitBegin + 128;
    private static final int cleanLengthBegin = cleanResourceBegin + 128;

    private static final int ebpBeginner = cleanLengthBegin + 2;
    /**
     * 记录栈顶的储存位置
     */
    private static final int espBeginner = ebpBeginner + 2;

    private static final int pcb_tidBeginner = 0;
    private static final int pcb_runningBeginner = 2;
    private static final int pcb_leftTimeBeginner = 3;
    private static final int pcb_rsLengthBeginner = 5;
    private static final int pcb_resourceBeginner = 6;

    @Override
    public void ProcessSchedule(Task[] arrivedTask, int[] cpuOperate) {
        int esp = recordTasks(arrivedTask);
        int ebp = readShortResource(ebpBeginner);
        int initEBP = ebp;
        int cpuNumber = getCpuNumber();
        int cpuCount = 0;


        boolean clean = false;
        for (int i = ebp; i < esp && cpuCount < cpuNumber; ) {
            int isRunning = readByte(i + pcb_runningBeginner);
            int leftTime = readShort(i + pcb_leftTimeBeginner);
            int resourceLength = readByte(i + pcb_rsLengthBeginner) + 1;
            //上次已经占有CPU 故而继续占有CPU 无需检测资源
            if (isRunning > 0 && leftTime > 0) {
                int tid = readShort(i + pcb_tidBeginner);
                leftTime--;
                cpuOperate[cpuCount++] = tid;
                writeShort(i + pcb_leftTimeBeginner, leftTime);
                if (leftTime == 0) {
                    clean = true;
                    cleanResources(i, resourceLength);
                }
            }//否则检测资源是否可用
            else if (leftTime > 0 && useResources(i, resourceLength)) {
                int tid = readShort(i + pcb_tidBeginner);
                leftTime--;
                cpuOperate[cpuCount++] = tid;
                writeShort(i + pcb_leftTimeBeginner, leftTime);
                if (leftTime == 0) {
                    clean = true;
                    cleanResources(i, resourceLength);
                } else
                    writeByte(i + pcb_runningBeginner, 1);

            }
            if (leftTime == 0 && ebp == i)
                ebp += pcb_resourceBeginner + resourceLength;
            i += pcb_resourceBeginner + resourceLength;
        }

        if (clean) {
            clean();
        }
        if (ebp > (Short.MAX_VALUE - MEM_SIZE) && esp > (Short.MAX_VALUE - MEM_SIZE)) {
            writeShortResource(ebpBeginner, ebp % MEM_SIZE);
            writeShortResource(espBeginner, esp % MEM_SIZE);
            return;
        }
        if (ebp > initEBP)
            writeShortResource(ebpBeginner, ebp);
    }

    /**
     * 记录到达的tasks信息 并返回 esp
     *
     * @param tasks
     * @return
     */
    private int recordTasks(Task[] tasks) {
        int esp = readShortResource(espBeginner);
        if (tasks == null || tasks.length == 0)
            return esp;
        for (int i = 0; i < tasks.length; i++) {
            writeShort(esp + pcb_tidBeginner, tasks[i].tid);
            writeByte(esp + pcb_runningBeginner, 0);
            writeShort(esp + pcb_leftTimeBeginner, tasks[i].cpuTime);
            writeByte(esp + pcb_rsLengthBeginner, tasks[i].resource.length - 1);
            for (int j = 0; j < tasks[i].resource.length; j++) {
                writeByte(esp + pcb_resourceBeginner + j, tasks[i].resource[j] - 1);
            }
            esp += pcb_resourceBeginner + tasks[i].resource.length;
        }
        writeShortResource(espBeginner, esp);
        return esp;
    }

    private boolean useResources(int taskBeginner, int resourceLength) {
//        int tid = readShort(taskBeginner + pcb_tidBeginner);
//        System.out.print(tid + " is trying to use ");
        for (int i = 0; i < resourceLength; i++) {
            int temple = readByte(taskBeginner + pcb_resourceBeginner + i);
//            System.out.print(temple + " ");
            if (readByteResource(resourceBitBegin + temple) != 0) {
//                System.out.println("fail use " + temple);
                return false;
            }
        }
//        System.out.println();
//        System.out.print(tid + " is  using ");
        for (int i = 0; i < resourceLength; i++) {
            int temple = readByte(taskBeginner + pcb_resourceBeginner + i);
//            System.out.print(temple + " ");
            writeByteResource(resourceBitBegin + temple, 1);
        }
//        System.out.println();
        return true;
    }

    private void cleanResources(int taskBeginner, int resourceLength) {
        int cleanLength = readShortResource(cleanLengthBegin);
//        System.out.print("is ready to clean");
        for (int i = 0; i < resourceLength; i++) {
            int temple = readByte(taskBeginner + pcb_resourceBeginner + i);
//            int x = readByteResource(resourceBitBegin + temple);
//            System.out.print(" " + temple + " " + (x == 0 ? true : false));
            writeByteResource(cleanResourceBegin + cleanLength + i, temple);
        }

        cleanLength += resourceLength;
//        System.out.println(" now resourceLength is " + cleanLength);

        writeShortResource(cleanLengthBegin, cleanLength);
    }

    private void clean() {
        int cleanLength = readShortResource(cleanLengthBegin);
//        System.out.println("is cleaning " + cleanLength + " time tick" + getTimeTick());
        for (int i = 0; i < cleanLength; i++) {
            int temple = readByteResource(cleanResourceBegin + i);
            writeByteResource(temple + resourceBitBegin, 0);
//            System.out.print(temple + " ");
        }
        cleanLength = 0;
        writeShortResource(cleanLengthBegin, cleanLength);
    }

    /**
     * 自由读取
     *
     * @param beginIndex
     * @return
     */
    private int readByteResource(int beginIndex) {
        return readFreeMemory(beginIndex);
    }

    /**
     * 自由写入
     *
     * @param beginIndex
     * @param value
     */
    private void writeByteResource(int beginIndex, int value) {
        writeFreeMemory(beginIndex, (byte) value);
    }

    /**
     * 自由写入2字节
     *
     * @param beginIndex
     * @param value
     */
    private void writeShortResource(int beginIndex, int value) {
        writeFreeMemory(beginIndex + 1, (byte) ((value & 0x000000ff)));
        writeFreeMemory(beginIndex, (byte) ((value & 0x0000ff00) >> 8));
    }

    /**
     * 自由写入2字节
     *
     * @param beginIndex
     * @return
     */
    private int readShortResource(int beginIndex) {
        int ans = 0;
        ans += (readFreeMemory(beginIndex) & 0xff) << 8;
        ans += (readFreeMemory(beginIndex + 1) & 0xff);
        return ans;
    }

    /**
     * 受限读取
     *
     * @param beginIndex
     * @return
     */
    private int readShort(int beginIndex) {
        int ans = 0;
        ans += (readFreeMemory(beginIndex % resourceBitBegin) & 0xff) << 8;
        ans += (readFreeMemory((beginIndex + 1) % resourceBitBegin) & 0xff);
        return ans;
    }

    /**
     * 受限写入
     *
     * @param beginIndex
     * @param value
     */
    private void writeShort(int beginIndex, int value) {
        writeFreeMemory((beginIndex + 1) % resourceBitBegin, (byte) ((value & 0x000000ff)));
        writeFreeMemory(beginIndex % resourceBitBegin, (byte) ((value & 0x0000ff00) >> 8));
    }

    /**
     * 读取一个字节
     */
    private int readByte(int beginIndex) {
        return readFreeMemory(beginIndex % resourceBitBegin);
    }

    /**
     * 写一个字节
     *
     * @param beginIndex
     * @param value
     */
    private void writeByte(int beginIndex, int value) {
        writeFreeMemory(beginIndex % resourceBitBegin, (byte) value);
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
        int cpuNumber = 4;
        // 定义测试文件
        String filename = "src/testFile/rand_7.csv";
        BottomMonitor bottomMonitor = new BottomMonitor(filename, cpuNumber);
        BottomService bottomService = new BottomService(bottomMonitor);
        Schedule schedule = new S161250036();
        schedule.setBottomService(bottomService);

        //外部调用实现类
        for (int i = 0; i < 10000; i++) {
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
