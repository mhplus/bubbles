# -*- coding: cp949 -*-
import math
class Vector(tuple):
	def __sub__(self, vector):
		return Vector( i - j for i, j in zip(self, vector) )

	def __add__(self, vector):
		return Vector( i + j for i, j in zip(self, vector) )

	def __mul__(self, value):
		return Vector( i*value for i in self)

	def normalize(self):
		size = self.magnitude()
		return Vector( i/size for i in self )

	def magnitude(self):
		sum2 = 0
		for i in self:
			sum2 += i * i
		return math.sqrt(sum2)

	def dot(self, vector):
		sum = 0
		for i, j in zip(self, vector):
			sum += i * j
		return sum

	def get_angle(self, vector=None):
		# cos(x) = n1.dot(n2)
		n1 = self.normalize()
		if vector:
			n2 = vector.normalize()
		else:
			n2 = Vector((1, 0))

		cos_theta = n1.dot(n2)
		theta = math.acos(cos_theta)
		if 0 < self[1]:
			return -theta * 180 / math.pi
		else:
			return theta * 180 / math.pi

	def rotate(self, angle):
		theta = angle * math.pi / 180
		cos_theta = math.cos(theta)
		sin_theta = math.sin(theta)

		x, y = self
		y *= -1
		x1 = x * cos_theta - y * sin_theta
		y1 = x * sin_theta + y * cos_theta
		return Vector((x1, -y1))

if __name__ == '__main__':
	v1 = Vector((1,0))
	v2 = Vector((0,-1))
	v3 = Vector((-1,0))
	v4 = Vector((0,1))
	v5 = Vector((1.3008572759035593, 1.300857275903559))

	print 'v1 :', v1, v1.get_angle()
	print 'v2 :', v2, v2.get_angle()
	print 'v3 :', v3, v3.get_angle()
	print 'v4 :', v4, v4.get_angle()
	print 'v5 :', v5, v5.get_angle()

	print 'v1.rotate(45) :', v1.rotate(45).get_angle()
	print 'v1.rotate(135) :', v1.rotate(135).get_angle()
	print 'v1.rotate(225) :', v1.rotate(225).get_angle()
	print 'v1.rotate(315) :', v1.rotate(315).get_angle()
	print 'v1.rotate(-90) :', v1.rotate(-45).get_angle()
	print 'v5.rotate(-90) :', v5.rotate(-90)
	print 'v5.rotate(-90).angle :', v5.rotate(-90).get_angle()

