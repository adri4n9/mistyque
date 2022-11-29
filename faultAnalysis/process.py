def processoutput(output, blocksize):
    i=int(b''.join([x for x in output.split(b'\n') if x.find(b'<<<<<')==0][0][10:].split(b' ')), 16)
    return i
    
    
with open('out.txt', 'r') as file:
    out = file.read()

print(out)
x = processoutput(out, 16)

print ("x")
