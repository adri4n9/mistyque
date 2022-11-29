import deadpool_dfa
import phoenixAES


target ='mystique/WBCTargets/faulted/libWBCexample.so'
bkp ='mystique/WBCTargets/faulted/libWBCexample.so.golden'

command = './targetWrapper.sh'



def processoutput(output, blocksize):
    data = b''.join([x for x in output.split(b'\n') if x.find(b'<<<<<')==0][0][5:].split(b' ')).decode('ASCII')
    return int(data,16)

engine=deadpool_dfa.Acquisition(targetbin=command, processoutput=processoutput, targetdata=target, goldendata=bkp, maxleaf=100, dfa=phoenixAES)
tracefiles=engine.run()
for tracefile in tracefiles[0]:
    print(tracefile)
    if phoenixAES.crack_file(tracefile,verbose=9):
        break
