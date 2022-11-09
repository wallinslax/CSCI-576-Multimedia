from scipy.fftpack import dct
import numpy as np
import math
def getSuv(input, u: int, v: int)-> int:
    s = 0.0
    for i in range(0, 8):
        for j in range(0, 8):
            if u == 0 and v == 0:
                s += (1/4) * (1/2) * input[i][j]
            elif  u == 0 or  v == 0:
                s += (1/4) *(input[i][j] * math.cos((2*i+1)*u*np.pi/16)*math.cos((2*j+1)*v*np.pi/16)) * (1/math.sqrt(2))
            else:
                s += (1/4) *(input[i][j] * math.cos((2*i+1)*u*np.pi/16)*math.cos((2*j+1)*v*np.pi/16))
    return s 

if __name__ == "__main__":
    a = np.array([
        [188,180,155,149,179,116,86,96],
        [168,179,168,174,180,111,86,95],
        [150,166,175,189,165,101,88,97],
        [163,165,179,184,135,90,91,96],
        [170,180,178,144,102,87,91,98],
        [175,174,141,104,85,83,88,96],
        [153,134,105,82,83,87,92,96],
        [117,104,86,80,86,90,92,103]])
    np.set_printoptions(precision=1, suppress=True)
    # print(a)
    # https://docs.scipy.org/doc/scipy/reference/generated/scipy.fftpack.dct.html
    #     
    dct_aOrtho = dct(dct(a,axis=0,norm='ortho'),axis = 1, norm='ortho')
    print('dct_aOrtho=',dct_aOrtho)
    # dct_a = dct (dct(a,axis=0),axis = 1)
    # print('dct_a=',dct_a)
    qTable = [
        [16,11,10,16,24,40,51,61],
        [12,12,14,19,26,58,60,55],
        [14,13,16,24,40,57,69,56],
        [14,17,22,29,51,87,80,62],
        [18,22,37,56,68,109,103,77],
        [24,35,55,64,81,104,113,92],
        [49,64,78,87,103,121,120,101],
        [72,92,95,98,112,100,103,99]
    ]
    dct_a = np.zeros((8,8))
    quantized_dct_a = np.zeros((8,8))
    for r in range(8):
        for c in range(8):
            dct_a[r][c] = getSuv(a, r,c)
            quantized_dct_a[r][c] = round(dct_a[r][c]/qTable[r][c])
    print('dct_a=',dct_a)
    print('quantized_dct_a=',quantized_dct_a)
