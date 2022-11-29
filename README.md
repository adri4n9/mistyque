# Mystique
* Mystique is a shape-shifter capable of taking several form.

Mystique pretends to be an Android device and manipulates their victims to trust in her. Once she gains her trust she can obtain all their secrets by generating a memory trace.

* MainActivity64 and MainActivity provide examples of trace generation for Android white-box crypto .so binary files

Mystique traces are compatible with side-channel marvel deadpool and Daredevil analysis tools scripts.

DFA is also possible by using Jean Grey  and Deadpool
an example is provided in MainActivityDFA.

thanks to all the contributos

this tool has been developed as a module to the amazing unidgb project by zhkl0228 

https://github.com/zhkl0228/unidbg

all credit an kudos to him.

## Set-up

### Get unidgb running
unidgb and as consequence Mystique is better executed in [IntelliJ IDEA](https://www.jetbrains.com/idea).

Unidbg is a reverse tool based on unicorn, which can directly call so files in Android and IOS on the PC side
#### 2.Import project into idea

first, we need to import the Unidbg project in IDEA you can do tis by creating a new project from version control.
in IDEA
File>New> Project from Version Control
* https://github.com/zhkl0228/unidbg
* https://github.com/adri4n9/unidbg
#### 2.test
Next, you can import the project for the first time. Some jar packages will be downloaded automatically, which is related to network speed and Maven server. Please wait patiently
here is a ttencrypt test case in the path of unidbg Android \ SRC \ test \ Java \ com \ byedance \ frameworks \ core \ encrypt in the project, which directly executes the main method
The console prints relevant call information, indicating that the project is imported successfully