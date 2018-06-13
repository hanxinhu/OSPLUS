#操作系统加分项说明
[TOC]
## 调度方式说明
FCFS 先来先服务算法 
使用资源时采用贪心算法，先来的先占用资源
## 进程信息记录
### pcb包含内容
为了最大程度的精简信息，将pcb的内容简化至
tid 2 字节 任务id
isRunning 1 字节 是否上次调度时已经在运行 是的话继续占有资源运行，否则检测资源是否可用，如可用且有cpu可用，则该进程运行
leftTime 2 字节 剩余时间
resourceLength 1 字节 用于记录 长度（最大值为128，故存取时先减去1防止溢出，读取时加一）
resourceBegin n字节 用于记录 存取的 资源id， 值存取同上
### pcb存取方式
 采用ebp和esp分别记录栈顶和栈底，并封装读取内存函数，采用类似循环列表的思想，通过取余运算，使得读取内存时不会溢出。并为了防止esp,ebp溢出，
 在ebp,esp均可取余时，进行取余操作。每当位于栈底的任务完成，ebp便会增加，等价于此处pcb被清空，同理存取pcb时，esp也在不断增加。
## 资源管理
采用贪心算法，如果tid's isRunning>0,则继续占有资源,否则检测资源是否可用,若可用,则将所需资源位设为1
同时在tid完成时,使用cleanResource记录需要释放的资源id,使用cleanLength记录需要释放的资源长度,在每个时间片末尾
如果cleanLength>0 则开始释放资源。