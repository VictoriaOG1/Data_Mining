import random
import networkx as nx
import matplotlib.pyplot as plt
import numpy as np


##Creacion de clase Node
class WebNode:
    def __init__(self, number):
        self.outLinks = []
        self.inLinks = []
        self.pageRank = 1.0
        self.number = number

    def addOutLink(self, nodo):
        nodo.inLinks.append(self)
        self.outLinks.append(nodo)

    def getOutLinks(self):
        return self.outLinks

    def getInLinks(self):
        return self.inLinks

    def setRank(self, rank):
        self.pageRank = rank

    def getRank(self):
        return self.pageRank

    def getNumber(self):
        return self.number

    def printInfo(self):
        exists = []
        print("NODO NUMERO ", self.number)
        for item in self.outLinks:
            exists.append(item.getNumber())
        print("SALIDA A NODOS: ", exists)

    def getVotes(self):
        if len(self.outLinks) == 0:
            return 0
        else:
            return 1 / len(self.outLinks)


# Creacion de grafo
num_nodes = 15
nodeList = [WebNode(i) for i in range(num_nodes)]
for node in nodeList:
    connections = random.randint(1, 3)
    nodeNumbers = list(range(num_nodes))
    for i in range(connections):
        choice = random.choice(nodeNumbers)
        node.addOutLink(nodeList[choice])
        nodeNumbers.remove(choice)

for node in nodeList:
    if len(node.getInLinks()) == 0:
        randomCon = random.choice(
            [x for x in range(num_nodes) if x != node.getNumber()]
        )
        nodeList[randomCon].addOutLink(node)

for nnode in nodeList:
    lista = nnode.getInLinks()
    if len(lista) == 0:
        print("HAY SIN ENTRADAS", nnode.getNumber())

## Creacion de las trampas
randomNodes = list(range(num_nodes))
trapNodes = []
for i in range(5):
    randomNode = random.choice(randomNodes)
    trapNodes.append(randomNode)
    randomNodes.remove(randomNode)

newNodes = []
for i in range(6):
    newNode = WebNode(i + num_nodes)
    newNodes.append(newNode)

# SpiderTraps
nodeList[trapNodes[0]].addOutLink(newNodes[0])
newNodes[0].addOutLink(newNodes[1])
newNodes[1].addOutLink(newNodes[0])

nodeList[trapNodes[1]].addOutLink(newNodes[2])
newNodes[2].addOutLink(newNodes[2])

# Dead ends
nodeList[trapNodes[2]].addOutLink(newNodes[3])
nodeList[trapNodes[3]].addOutLink(newNodes[4])
nodeList[trapNodes[4]].addOutLink(newNodes[5])

for item in newNodes:
    nodeList.append(item)

# Dibujo del grafo
G = nx.DiGraph()

for node in nodeList:
    G.add_node(node.getNumber())

# Anadir edges al grafo basado en los inLinks y outLinks
for node in nodeList:
    for neighbor in node.getOutLinks():
        G.add_edge(node.getNumber(), neighbor.getNumber())
    for neighbor in node.getInLinks():
        G.add_edge(neighbor.getNumber(), node.getNumber())

plt.figure(figsize=(10, 10))

# Calcula la posicion para poner todos los nodos del grafo en el grafico
pos = nx.spring_layout(G, k=3.0, seed=30)

# Dibuja el grafo
nx.draw(G, pos, with_labels=True, node_size=500, font_size=12, node_color="skyblue")

nx.draw_networkx_edges(G, pos, arrowsize=20)
plt.show()

## Creacion del vector r y el vector p(t)
rango = num_nodes + 6  # len(nodeList)
r = []
pt = []

for item in range(rango):
    r.append(1 / rango)
    pt.append(nodeList[item].getVotes())

## Obtencion de la matriz de adyacencia

# inicializa la matriz con 0
num_nodes = len(G.nodes)
adjacency_matrix = np.zeros((num_nodes, num_nodes))

# calcula las probabilidades (fracciones) y llena la matriz
for node in nodeList:
    neighbors = list(node.getOutLinks())
    if neighbors:
        fraction = 1.0 / len(neighbors)
        for neighbor in neighbors:
            adjacency_matrix[node.getNumber()][neighbor.getNumber()] = fraction

## Funcion del Random Walker
visitedNodes = []
notVisitedNodes = nodeList.copy()

fillValue = 1 / rango
m = np.full(adjacency_matrix.shape, fillValue)
newMatrix = (0.8 * adjacency_matrix) + (0.2 * m)


def randomWalker(randomWalkerNode, piRResult, piPResult, matrix, t):
    if len(notVisitedNodes) != 0:
        print("t: ", t)
        newPIRResult = powerIteratorR(piRResult, matrix)
        newPIPResult = powerIteratorP(piPResult, matrix)
        randomWalkerNode.printInfo()
        t = t + 1

        if randomWalkerNode not in visitedNodes:
            visitedNodes.append(randomWalkerNode)
            notVisitedNodes.remove(randomWalkerNode)

        inputNumber = input("Por favor, ingrese un número (1: teleport, 2: stay): ")
        output = randomWalkerNode.getOutLinks()
        try:
            if inputNumber == "1":
                if len(notVisitedNodes) != 0:
                    print("Se hizo teleport" + "\n")
                    pathToChoose = random.choice(notVisitedNodes)
                    randomWalker(pathToChoose, newPIRResult, newPIPResult, newMatrix, t)
            else:
                print("No se hizo teleport" + "\n")
                if len(output) == 0:
                    randomWalker(
                        randomWalkerNode, newPIRResult, newPIPResult, matrix, t
                    )
                else:
                    pathOptions = randomWalkerNode.getOutLinks()
                    pathToChoose = random.choice(pathOptions)
                    randomWalker(pathToChoose, newPIRResult, newPIPResult, matrix, t)
        except ValueError:
            print("Por favor, ingrese un número válido.")
    else:
        print("TODOS LOS NODOS HAN SIDO VISITADOS.")
        for item in visitedNodes:
            print("Visited node: ", item.getNumber())


## Funcion Power Iterator
def powerIteratorR(vectorR, ADMatrix):
    print("vector r: ", np.round(vectorR, 3))
    newVectorR = ADMatrix.dot(vectorR)
    # Normalize the vector
    sum_vectorR = np.sum(newVectorR)
    newVectorR = newVectorR / sum_vectorR
    return newVectorR


## Funcion Power Iterator
def powerIteratorP(vectorPT, ADMatrix):
    print("vector p(t): ", np.round(vectorPT, 3))
    newVectorPT = ADMatrix.dot(vectorPT)
    return newVectorPT


## Esocoge el primer nodo al azar
format_string = "{:.2f}"
print("Adjacency matrix: ")
for row in adjacency_matrix:
    for element in row:
        print(format_string.format(element), end=" ")
    print()
print("\n")
firstNode = random.choice(nodeList)
randomWalker(firstNode, r, pt, adjacency_matrix, 0)
