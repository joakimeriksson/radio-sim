#!/usr/bin/python
import sys,os,subprocess,time
import xml.etree.ElementTree as ET

class Type(object):
    def __init__(self, mtype):
        self.desc = mtype.find('description').text
        self.id = mtype.find('identifier').text
        self.command = mtype.find('commands').text
        self.firmware = mtype.find('firmware').text
        self.nodes = []
    def out(self):
        print "Type    :", self.desc," ID:",self.id
        print "Command :", self.command
        print "Firmware:", self.firmware
    def make(self,path):
        print "BUILDING IN", path, " CMD: ", self.command
        make_process = subprocess.Popen(self.command, shell=True, stderr=subprocess.STDOUT, cwd=path)
        if make_process.wait() != 0:
            print "ERROR!!!"
            sys.exit(1)

class Node(object):
    def __init__(self, mote):
        self.id = mote.find('interface_config/id').text
        self.x = mote.find('interface_config/x').text
        self.y = mote.find('interface_config/y').text
        self.z = 0.0
        self.type = mote.find('motetype_identifier').text

    def yaml(self):
        return "{id: %s, position: [%s,%s,%s]}" % (self.id, self.x, self.y, self.z)
    def __repr__(self):
        return "Node: " + self.id + " of " + self.type + " pos: " + self.x + "," + self.y

loadedTypes = {}
allNodes = {}
coojaFile = sys.argv[1]
path = os.path.dirname(os.path.abspath(coojaFile))
# find Contiki in the path - TODO: this must be configurable...
contiki = path[:path.find("contiki") + len("contiki")]
tree = ET.parse(coojaFile)
root = tree.getroot()
print "**** Simulation :", root.find('simulation/title').text, "****"
print "     RadioMedium:", root.find('simulation/radiomedium').text.strip()
print "     Sim Path   :", path
print "     Contiki    :", contiki
print "****"
# Load and build the types
root = tree.getroot()
types = root.iter('motetype')
for type in types:
    t = Type(type)
    t.out()
    t.make(path)
    loadedTypes[t.id] = t
# Load the nodes
root = tree.getroot()
motes = root.iter('mote')
for mote in motes:
    n = Node(mote)
    print n
    allNodes[n.id] = n
    loadedTypes[n.type].nodes.append(n)

root = tree.getroot()
script = root.find('plugin/plugin_config/script').text
lcount = script.count('\n')
print " === SCRIPT === (",lcount,")"
print script
print " =============="

# Start the simulation - assumed to be from mspsim-emulink/test!
main = subprocess.Popen(['java', '-cp', 'emul8-radio-medium.jar', 'se.sics.emul8.radiomedium.script.CoojaScriptEngine'], stderr=subprocess.STDOUT, stdin=subprocess.PIPE, cwd="../../radio-medium")
# send in the data to the input of the sub-process...
time.sleep(5)

#for key in allNodes:
#    n = allNodes[key]
#    fw = loadedTypes[n.type].firmware.replace("[CONTIKI_DIR]",contiki)
#    print "Loading for ",n.id, " fw: ", fw
#    n1 = subprocess.Popen(['java', '-jar', 'mspsim-emulink.jar',fw,"-id=" + n.id], stderr=subprocess.STDOUT, cwd="../../mspsim-emulink")

# create a load-all nodes of the same type into the same MSPSim runner

for key in loadedTypes:
    fw = loadedTypes[key].firmware.replace("[CONTIKI_DIR]",contiki)
    print "Loading for ",n.id, " fw: ", fw
    n1 = subprocess.Popen(['java', '-jar', 'mspsim-emulink.jar',fw,"-yaml"], stderr=subprocess.STDOUT, stdin=subprocess.PIPE, cwd="../../mspsim-emulink")
    yamlconf = "nodes:\n"
    for node in loadedTypes[key].nodes:
        print "Generating start info for node:", node
        yamlconf += "- %s\n" % (node.yaml())
    yamlconf += "\n"
    print yamlconf
    n1.stdin.write(yamlconf)
    print "conf - done **** !"

# not needed - above test program steps automatically...
#print "******* starting test client that steps the simulation *******"
#testC = subprocess.Popen(['java', '-cp', 'emul8-radio-medium.jar', 'se.sics.emul8.radiomedium.test.TestClient'], stderr=subprocess.STDOUT, cwd="../../radio-medium")

time.sleep(5)
main.communicate(input="script:" + str(lcount) + "\n" + script + "\n")

time.sleep(120)
main.kill()
n1.kill()
testC.kill()
