#!/usr/bin/python
import coojacsc, sys
import subprocess,time

coojaFile = sys.argv[1]
(loadedTypes, allNodes, lcount, script, contiki) = coojacsc.cooja_load(coojaFile,"nodesim")
# Start the simulation - assumed to be from mspsim-emulink/test!
main = subprocess.Popen(['java', '-cp', 'emul8-radio-medium.jar', 'se.sics.emul8.radiomedium.script.CoojaScriptEngine'], stderr=subprocess.STDOUT, stdin=subprocess.PIPE, cwd="../../radio-medium")
# send in the data to the input of the sub-process...
time.sleep(5)

for key in loadedTypes:
    fw = loadedTypes[key].firmware.replace("[CONTIKI_DIR]",contiki)
    print "Loading for ",loadedTypes[key].nodes[0].id, " fw: ", fw
    for node in loadedTypes[key].nodes:
        print "Starting node:", node
        n1 = subprocess.Popen([fw, '-Dnodeid=' + node.id, '-Dx=' + str(node.x), '-Dy=' + str(node.y), '-Dz' + str(node.z)], stderr=subprocess.STDOUT, stdin=subprocess.PIPE, cwd="../../mspsim-emulink")


time.sleep(5)
main.communicate(input="script:" + str(lcount) + "\n" + script + "\n")

time.sleep(120)
main.kill()
n1.kill()
testC.kill()
