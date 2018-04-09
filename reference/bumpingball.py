# -*- coding: cp949 -*-
import struct
import runtime
import config
import keysym
import vector
import math

from psys import eloop
from pui import pyapp, widget

from pui.softkey import SoftKeyHandler
from runtime import trans_launcher as _

color_white = 255, 255, 255, 255
color_black = 0, 0, 0, 255

ball_color_blue = 86, 180, 235, 255
ball_color_orange = 255, 205, 99, 255
ball_color_green = 166, 224, 27, 255
ball_color_violet = 191, 168, 255, 255
ball_color_pink = 255, 151, 161, 255

ball_colors = ball_color_blue, ball_color_orange, ball_color_green, ball_color_violet, ball_color_pink

DEFAULT_SPEED = 35
screen_height = runtime.app_height + runtime.indicator_height

class BumpingBallScreen(object):
	def __init__(self, pos, size, bg):
		args = ('bumpingball', '-x', pos[0], '-y', pos[1], \
			'-w', size[0], '-h', size[1], '-i', bg)
		#args = ('valgrind', '--tool=memcheck', 'bumpingball', '-x', pos[0], '-y', pos[1], \
			#'-w', size[0], '-h', size[1], '-d', runtime.db['screen.colordepth'])

		from psys import utils
		self._screen = utils.spawn_with_mchannel(args, self.hup_handler)
		self._screen.set_handler('', self.handle_message)

	def send(self, *args):
		if self._screen:
			self._screen.send(*args)
		else:
			print self, '[BumpingBallScreen] send: app is not active'

	def hup_handler(self):
		bt()
		print 'bumpingball screen hup!!!'

	def handle_message(self, cmd, *args):
		print 'bumpingball screen handle_message', cmd, args

	def show(self):
		self.send('show')

	def hide(self):
		self.send('hide')

	def shape_update(self, name, id, *args):
		self.send('shape.update', name, id, *args)

	def shape_new(self, name, id, *args):
		self.send('shape.new', name, id, *args)

	def shape_destroy(self, name, id):
		print 'shape destroy', name, id
		self.send('shape.destroy', name, id)

	def destroy(self):
		self._screen.destroy()
		self._screen = None

class MoveAction(object):
	#time_slice = 160 / 1000.
	time_slice = 250 / 1000.
	acceleration = 0.021
	def __init__(self, shapes, speed, angle, cb=None):
		self.shapes = shapes
		self.set_angle(angle)

		self.speed = speed
		self.cb = cb

	def _get_dxy(self, d):
		dx = d * self.cos_theta
		dy = d * self.sin_theta
		return dx, -dy

	def set_angle(self, angle):
		self.angle = angle
		self.theta = angle * (math.pi / 180)
		self.cos_theta = math.cos(self.theta)
		self.sin_theta = math.sin(self.theta)

	def reset(self, speed, angle):
		self.set_angle(angle)
		self.speed = speed

	def get_bound_vectors(self, ball, move_vector):
		for o in self.shapes['objects']:
			vectors = o.get_bound_vectors(ball, move_vector)
			if vectors:
				contact_vector = vectors[0]

				old_x, old_y = ball.pos
				v_size = contact_vector.magnitude()
				vn = contact_vector.normalize()
				dx, dy = vn * (v_size + ball.r)
				pos = old_x + dx, old_y + dy
				self.cb('ball-contact', ball.color, o.color, pos)
				return vectors

		for w in self.shapes['walls']:
			vectors = w.get_bound_vectors(ball, move_vector)
			if vectors:
				return vectors

	def is_collided(self, ball, r):
		for o in self.shapes['objects']:
			if o.is_collided(ball, r):
				return True

		for w in self.shapes['walls']:
			if w.is_collided(ball, r):
				return True
		return False

	def check_collision(self, shape, move_vector):
		cinfo = self.get_bound_vectors(shape, move_vector)
		while cinfo:
			print 'cinfo', cinfo
			acceleration = self.__s * self.acceleration * 2
			self.__s -= acceleration
			contact_vector, bound_vector = cinfo
			print 'bound_vector', bound_vector
			contact_pos = contact_vector + shape.pos
			shape.pos = contact_pos
			if bound_vector == 'gameover':
				self.run_tag = None
				color = shape.color
				pos = shape.pos
				shape.destroy()
				self.cb('gameover', color, pos)
				return
			if not bound_vector.magnitude():
				print 'bound_vector size', bound_vector.magnitude()
				self.run_tag = None
				return
			new_angle = bound_vector.get_angle()
			print 'new_angle', new_angle
			self.set_angle(int(new_angle))
			cinfo = self.get_bound_vectors(shape, bound_vector)
			move_vector = bound_vector
			#print 'shape', shape.pos, 'new_angle', new_angle
		new_pos = move_vector + shape.pos
		shape.pos = new_pos

	def run(self, shape):
		self.__s = self.speed
		def _move():
			acceleration = self.__s * self.acceleration
			self.__s -= acceleration
			d = self.__s * self.time_slice

			if self.__s < 2:
				self.run_tag = None
				r = shape.r
				while not self.is_collided(shape, r):
					r += 1
				shape.expand(r)
				return False
				
			dx, dy = self._get_dxy(d)
			move_vector = vector.Vector((dx, dy))

			#import time
			#s = time.time()
			self.check_collision(shape, move_vector)
			#print 'collision check elapsed: ', time.time() - s
			return True
		self.run_tag = eloop.Timer(30, _move)

	def destroy(self):
		self.run_tag = None

class FallDownAction(object):
	expire_time = 3 * 1000
	time_slice = 50
	def __init__(self, finished_cb):
		self.run_tag = None
		self.duration = 0
		self.finished_cb = finished_cb
		self.shape = None

	def run(self, shape, vector):
		self.shape = shape
		r,g,b,a = shape.color
		shape.color = r, g, b, int(a * .3)
		dt = float(self.expire_time) / self.time_slice
		nv = vector.normalize()
		def _move():
			self.duration += self.time_slice
			if self.expire_time <= self.duration:
				self.run_tag = None
				shape.destroy()
				self.finished_cb()
				return False
			dx, dy = nv * 5
			x, y = shape.pos
			shape.pos = x + dx, y + dy
			return True
		self.run_tag = eloop.Timer(self.time_slice, _move)

	def destroy(self):
		if self.shape:
			self.shape.destroy()
		self.run_tag = None

class FragmentFallDownAction(object):
	expire_time = 2 * 1000
	time_slice = 50
	def __init__(self, finished_cb):
		self.run_tag = None
		self.duration = 0
		self.finished_cb = finished_cb
		self.shape = None
		import random
		self.rotation = ('left', 'right')[random.randint(0, 1)]

	def run(self, shape, vector):
		self.shape = shape
		dt = float(self.expire_time) / self.time_slice
		nv = vector.normalize()
		def _move():
			self.duration += self.time_slice
			if self.expire_time <= self.duration:
				self.run_tag = None
				self.finished_cb(shape)
				return False
			dx, dy = nv
			x, y = shape.pos
			shape.pos = x + dx, y + dy
			if self.rotation == 'left':
				shape.angle = (shape.angle - 20) % 360
			else:
				shape.angle = (shape.angle + 20) % 360
			return True
		self.run_tag = eloop.Timer(self.time_slice, _move)

	def destroy(self):
		if self.run_tag:
			self.finished_cb(self.shape)
		self.run_tag = None

class Shape(widget.Widget):
	name = 'shape'
	_color = color_white
	refrence_count = 0

	def __init__(self, parent, screen, pos=(0.0, 0.0)):
		self.__super.__init__(parent)
		self.__pos = pos
		self.screen = screen
		self.id = Shape.refrence_count
		Shape.refrence_count += 1
		self.init_shape()
		self.pos = pos

	def init_shape(self):
		assert True

	def update(self):
		assert True

	def get_pos(self):
		return self.__pos
	def set_pos(self, pos):
		if self.__pos == pos:
			return
		self.__pos = pos
		self.update()
	pos = property(get_pos, set_pos)

	def get_color(self):
		return self._color
	def set_color(self, color):
		if self._color == color:
			return
		self._color = color
		self.update()
	color = property(get_color, set_color)

	def destroy(self):
		self.screen.shape_destroy(self.name, self.id)
		self.run_callback('destroy', self)
		self.__super.destroy()

class Text(Shape):
	name = 'text'
	def __init__(self, parent, screen, text='', pos=(0.0, 0.0)):
		self.__text = text
		self.__font_size = 10
		self.__super.__init__(parent, screen, pos)
		self.color = color_black

	def init_shape(self):
		color, = struct.unpack('>i', struct.pack('BBBB', *self.color))
		x, y = self.pos
		self.screen.shape_new(self.name, self.id, self.__text, self.__font_size, float(x), float(y), color)

	def update(self):
		color, = struct.unpack('>i', struct.pack('BBBB', *self.color))
		x, y = self.pos
		self.screen.shape_update(self.name, self.id, self.__text, self.__font_size, float(x), float(y), color)

	def get_text(self):
		return self.__text
	def set_text(self, text):
		if self.__text == text:
			return
		self.__text = text
		self.update()
	text = property(get_text, set_text)

	def get_font_size(self):
		return self.__font_size
	def set_font_size(self, font_size):
		if self.__font_size == font_size:
			return
		self.__font_size = font_size
		self.update()
	font_size = property(get_font_size, set_font_size)

class Fragment(Shape):
	name = 'fragment'
	def __init__(self, parent, screen, size, angle, color, pos=(0, 0)):
		self._color = color
		self.__size = size
		self.__angle = angle
		self.__super.__init__(parent, screen, pos=pos)

	def init_shape(self):
		color, = struct.unpack('>i', struct.pack('BBBB', *self.color))
		x, y = self.pos
		self.screen.shape_new(self.name, self.id, float(x), float(y),
					self.size, self.angle, color)

	def update(self):
		color, = struct.unpack('>i', struct.pack('BBBB', *self.color))
		x, y = self.pos
		self.screen.shape_update(self.name, self.id, float(x), float(y),
					self.size, self.angle, color)

	def get_size(self):
		return self.__size
	def set_size(self, size):
		if self.__size == size:
			return
		self.__size = size
		self.update()
	size = property(get_size, set_size)

	def get_angle(self):
		return self.__angle
	def set_angle(self, angle):
		if self.__angle == angle:
			return
		self.__angle = angle
		self.update()
	angle = property(get_angle, set_angle)

class Ball(Shape):
	name = 'ball'
	def __init__(self, parent, screen, color, pos=(0, 0)):
		self.__bump_count = 3
		self.__radius = 10
		self._color = color
		self.__super.__init__(parent, screen, pos=pos)
		self.effect = None

	def init_shape(self):
		color, = struct.unpack('>i', struct.pack('BBBB', *self.color))
		x, y = self.pos
		self.screen.shape_new(self.name, self.id, float(x), float(y), self.r, self.bump_count, color)

	def update(self):
		color, = struct.unpack('>i', struct.pack('BBBB', *self.color))
		x, y = self.pos
		self.screen.shape_update(self.name, self.id, float(x), float(y), self.r, self.bump_count, color)

	def get_distance(self, pos):
		x1, y1 = self.pos
		x2, y2 = pos
		dx = x2 - x1
		dy = y2 - y1
		return math.sqrt(dx*dx + dy*dy)

	def set_alpha(self, alpha):
		r, g, b, a = self.color
		self.color = r, g, b, int(255 * alpha)

	def get_bump_count(self):
		return self.__bump_count
	def set_bump_count(self, bump_count):
		if self.__bump_count == bump_count:
			return
		self.__bump_count = bump_count
		if bump_count == 2:
			self.set_alpha(0.7)
		elif bump_count == 1:
			self.set_alpha(0.4)
		self.update()
	bump_count = property(get_bump_count, set_bump_count)

	def get_radius(self):
		return self.__radius
	def set_radius(self, r):
		if self.__radius == r:
			return
		self.__radius = r
		self.update()
	r = property(get_radius, set_radius)

	def expand(self, r):
		def completed():
			self.effect = None
			self.r = r - 1
			self.run_callback('expand-finish', self)
		from pui.animation import EffectScheduler
		self.effect = EffectScheduler(400, 60, completed=completed, interp='cos')

		def cb(r):
			self.r = r
		self.effect.add_var('r' + str(self), self.r, r - 1, cb)

	def get_bound_vectors(self, ball, move_vector):
		# self : Ball A
		# ball : Ball B
		r1 = self.r
		r2 = ball.r
		new_pos = move_vector + ball.pos

		# step1 : vector의 크기가 A와 B의 거리보다 작으면 False
		move_vector_size = move_vector.magnitude()
		distance = self.get_distance(ball.pos)
		if move_vector_size <= distance - (r1 + r2):
			return False

		# step2 : move_vector의 정규벡터와 B-A vector(vc)의 dot product가
		#         0보다 작거나 같으면 B쪽으로 향하고 있지 않음.
		# d     : N dot C == |C| * cos(angle between N and C)
		#         the distance of vertical line between move_vector and B

		vb = vector.Vector(self.pos) 	# vector 0->b
		va = vector.Vector(ball.pos) 	# vector 0->a
		vc = vb - va 		# vector a->b

		vn = move_vector.normalize()
		d = vn.dot(vc)
		if d <= 0:
			return False

		# step 3 : B의 중점에서 move_vector 위로 내린 수선의 발의 길이(F)가
		#          두원의 반지름의 합보다 크면 충돌 아님
		#          F is the shortest distance of B on move_vector
		#          f2 == F*F
		distance_c = vc.magnitude()
		f2 = distance_c * distance_c - d * d
		sum_r = r1 + r2
		rsqure = sum_r * sum_r
		if rsqure <= f2:
			return False

		# step 4: A 와 B가 접할때 A의 중점과 move_vector 상의 F점과의거리 T가
		#         음수이면 충돌 아님
		t2 = rsqure - f2
		if t2 < 0:
			return False

		contact_distance = d - math.sqrt(t2)
		if (move_vector_size < contact_distance):
			return False

		#bt()
		print 'move_vector', move_vector
		print 'contact_distance', contact_distance

		extra_size = move_vector_size - contact_distance
		if extra_size == 0:
			bt()
			print 'd', d
			print 't2', t2
			return move_vector, vector.Vector((0, 0))
			
		contact_vector = vn * extra_size
		print 'contact_vector', contact_vector
		print 'contact_vector angle', contact_vector.get_angle()
		print 'vn==contact_vector angle', vn.get_angle()
		print 'r1 to r2 vc angle', vc, vc.get_angle()

		hc = vc.rotate(-90) # 접선 vector
		print 'hc (vc rotate -90) angle', hc.get_angle()
		bound_angle = hc.get_angle(vn)
		print 'bump_count', self.bump_count
		if bound_angle < 0:
			bound_angle = -bound_angle

		bound_vector = move_vector - contact_vector
		bound_vector = bound_vector.rotate(-bound_angle*2)
		print 'bound_vector', bound_vector
		print 'bound_angle', bound_vector, bound_angle
		self.bump_count -= 1
		if self.bump_count < 0:
			assert False, 'invalid bumpcount'
		if self.bump_count == 0:
			# 접점에서 target ball의 원점까지의 vector : v1
			# 
			v1 = vc - contact_vector
			vn = v1.normalize()
			self.run_callback('fall-down', self, vn * extra_size)
		return contact_vector, bound_vector

	def is_collided(self, ball, r2):
		r1 = self.r
		x1, y1 = self.pos

		x2, y2 = ball.pos

		sum_r = r1 + r2
		dx = x2 - x1
		dy = y2 - y1
		if dx * dx + dy * dy <= sum_r * sum_r: 
			return True
		return False

	def destroy(self):
		if self.effect:
			self.effect.finish()
		self.__super.destroy()
	
class Line(Shape):
	name = 'line'
	def __init__(self, parent, screen, spos=(0.0, 0.0), dpos=(0.0, 0.0)):
		self.__spos = spos
		self.__dpos = dpos
		self.__super.__init__(parent, screen)

	def init_shape(self):
		color, = struct.unpack('>i', struct.pack('BBBB', *self.color))
		sx, sy = self.spos
		dx, dy = self.dpos
		self.screen.shape_new(self.name, self.id, float(sx), float(sy), float(dx), float(dy), color)

	def update(self):
		color, = struct.unpack('>i', struct.pack('BBBB', *self.color))
		sx, sy = self.spos
		dx, dy = self.dpos
		self.screen.shape_update(self.name, self.id, float(sx), float(sy), float(dx), float(dy), color)

	def get_spos(self):
		return self.__spos
	def set_spos(self, spos):
		if self.__spos == spos:
			return
		self.__spos = spos
		self.update()
	spos = property(get_spos, set_spos)

	def get_dpos(self):
		return self.__dpos
	def set_dpos(self, dpos):
		if self.__dpos == dpos:
			return
		self.__dpos = dpos
		self.update()
	dpos = property(get_dpos, set_dpos)

	def get_pos(self):
		return self.__super.get_pos()
	def set_pos(self, pos):
		if self.pos == pos:
			return
		dx, dy = pos[0] - self.pos[0], pos[1] - self.pos[1]
		self.__spos = self.__spos[0] + dx, self.__spos[1] + dy
		self.__dpos = self.__dpos[0] + dx, self.__dpos[1] + dy
		self.__super.set_pos(pos)
		self.update()
	pos = property(get_pos, set_pos)

class LeftLine(Line):
	def is_collided(self, ball, r):
		cx, cy = ball.pos
		lx, ly = self.spos
		if cx - r <= lx:
			return True
		return False

	def get_bound_vectors(self, ball, move_vector):
		r = ball.r
		ox, oy = ball.pos
		lx, ly = self.spos
		dx, dy = move_vector
		cx = ox + dx 
		if cx - r <= lx:
			angle = move_vector.get_angle()
			dx = -(ox - r)
			dy = dx * math.tan(angle * math.pi / 180)
			contact_vector = vector.Vector((dx, -dy))
			total_distance = move_vector.magnitude()
			contact_distance = contact_vector.magnitude()
			d = total_distance - contact_distance

			hc = vector.Vector((d, 0))
			bound_angle = 180 - angle
			bound_vector = hc.rotate(bound_angle)
			return contact_vector, bound_vector

class RightLine(Line):
	def is_collided(self, ball, r):
		cx, cy = ball.pos
		lx, ly = self.spos
		if lx <= cx + r:
			return True
		return False

	def get_bound_vectors(self, ball, move_vector):
		r = ball.r
		ox, oy = ball.pos
		dx, dy = move_vector
		cx = ox + dx 
		lx, ly = self.spos
		if lx <= cx + r:
			angle = move_vector.get_angle()
			dx = lx - ox - r
			dy = dx * math.tan(angle * math.pi / 180)
			contact_vector = vector.Vector((dx, -dy))
			total_distance = move_vector.magnitude()
			contact_distance = contact_vector.magnitude()
			d = total_distance - contact_distance

			hc = vector.Vector((d, 0))
			bound_angle = 180 - angle
			bound_vector = hc.rotate(bound_angle)
			return contact_vector, bound_vector

class TopLine(Line):
	def is_collided(self, ball, r):
		cx, cy = ball.pos
		lx, ly = self.spos
		if cy - r <= ly:
			return True
		return False

	def get_bound_vectors(self, ball, move_vector):
		r = ball.r
		ox, oy = ball.pos
		dx, dy = move_vector
		lx, ly = self.spos
		cy = oy + dy
		if cy - r <= ly:
			angle = move_vector.get_angle()
			dy = -(oy - r)
			dx = dy * math.tan((angle - 90) * math.pi / 180)
			contact_vector = vector.Vector((dx, dy))
			total_distance = move_vector.magnitude()
			contact_distance = contact_vector.magnitude()
			d = total_distance - contact_distance

			hc = vector.Vector((d, 0))
			bound_angle = -angle
			bound_vector = hc.rotate(bound_angle)
			return contact_vector, bound_vector

class BottomLine(Line):
	def is_collided(self, ball, r):
		cx, cy = ball.pos
		lx, ly = self.spos
		if ly <= cy + r:
			return True
		return False

	def get_bound_vectors(self, ball, move_vector):
		r = ball.r
		ox, oy = ball.pos
		lx, ly = self.spos
		dx, dy = move_vector
		cy = oy + dy
		if ly <= cy + r:
			angle = move_vector.get_angle()
			dy = ly - oy - r
			dx = dy * math.tan((angle - 90) * math.pi / 180)
			contact_vector = vector.Vector((dx, dy))
			total_distance = move_vector.magnitude()
			contact_distance = contact_vector.magnitude()
			d = total_distance - contact_distance
			hc = vector.Vector((d, 0))
			bound_angle = -angle
			bound_vector = hc.rotate(bound_angle)
			return contact_vector, bound_vector

class DeadLine(Line):
	def is_collided(self, ball, r):
		cx, cy = ball.pos
		lx, ly = self.spos
		if ly <= cy + r:
			return True
		return False

	def get_bound_vectors(self, ball, move_vector):
		r = ball.r
		ox, oy = ball.pos
		lx, ly = self.spos
		dx, dy = move_vector
		cy = oy + dy
		if 0 < dy and ly <= cy + r:
			angle = move_vector.get_angle()
			dy = ly - oy - r
			dx = dy * math.tan((angle - 90) * math.pi / 180)
			contact_vector = vector.Vector((dx, dy))
			return contact_vector, 'gameover'

class Launcher(Shape):
	name = 'launcher'
	max_angle = 172
	min_angle = 8
	def __init__(self, parent, screen, pos=(0.0, 0.0)):
		self.__angle = 90
		self.__super.__init__(parent, screen, pos=pos)
		self.direction = 'left'
		self.color_index = 0
		self.reset_color()
		self.ball = Ball(self, self.screen, self.color, self.pos)

	def init_shape(self):
		color, = struct.unpack('>i', struct.pack('BBBB', *self.color))
		x, y = self.pos
		self.screen.shape_new(self.name, self.id, float(x), float(y), self.angle, color)

	def update(self):
		color, = struct.unpack('>i', struct.pack('BBBB', *self.color))
		x, y = self.pos
		self.screen.shape_update(self.name, self.id, float(x), float(y), self.angle, color)

	def get_angle(self):
		return self.__angle
	def set_angle(self, angle):
		if self.__angle == angle:
			return
		self.__angle = angle
		self.update()
	angle = property(get_angle, set_angle)

	def reset_color(self):
		i = self.color_index
		self.color = ball_colors[i]
		self.color_index = (i + 1) % len(ball_colors)

	def prepare_ball(self):
		def _move_ball():
			x, y = self.ball.pos 
			self.ball.pos = x, y - 1
			if self.ball.pos[1] <= self.pos[1]:
				self.ani_tag = None
				return False
			return True
		self.ani_tag = eloop.Timer(50, _move_ball)
		color = self.color
		x, y = self.pos
		pos = x, y + 20
		self.ball = Ball(self, self.screen, color, pos)

	def get_ball(self):
		ball = self.ball
		self.prepare_ball()
		return ball

	def start(self):
		self.reset_color()
		def _change_angle():
			if self.direction == 'left':
				angle = self.angle + 1
			else:
				angle = self.angle - 1
			if self.max_angle <= angle:
				self.direction = 'right'
				angle = self.max_angle
			elif angle <= self.min_angle:
				self.direction = 'left'
				angle = self.min_angle
				
			self.angle = angle
			return True
		self.__update_tag = eloop.Timer(20, _change_angle)

	def stop(self):
		self.__update_tag = None

class BumpingBallSoftKeyHandler(SoftKeyHandler):
	center_is_ok = True
	def get_keys(self):
		return '', _('New game'), _('Back')

	def show_menu(self, which):
		if which == 'center':
			self.owner.new_game()
			return True
		elif which == 'right':
			self.owner.close()
			return True

class BumpingBallWindow(pyapp.Window):
	softkey_class = BumpingBallSoftKeyHandler
	def __init__(self, app):
		self.__super.__init__(app)

		self.set_screen_type('no-indicator')
		self.screen = app.screen
		self.set_title(_('Bumping Ball'))
		self.dlg = None
		self.db = app.db

		self.game_state = 'playing' # 'idle', 'playing', 'gameover'
		self.ball = None
		self.fall_down_action = None
		self.move_action = None
		objects = []
		walls = []
		self.fragments = {}
		self.shapes = {'objects':objects, 'walls':walls}

		wall_infos = (
			(TopLine, 	(0, 0), (runtime.app_width, 0)),
			(LeftLine, 	(0, 0), (0, screen_height)),
			(RightLine, 	(runtime.app_width, 0), (runtime.app_width, screen_height)),
			#(BottomLine, 	(0, screen_height), (runtime.app_width, screen_height)),
			(DeadLine, 	(0, screen_height - 38), (runtime.app_width, screen_height - 38)),
		)
		for line_class, spos, dpos in wall_infos:
			l = line_class(self, self.screen)
			l.spos = spos
			l.dpos = dpos
			walls.append(l)
		self.init_score_labels()

		self.launcher = Launcher(self, self.screen, pos=(runtime.app_width/2, screen_height-10))
		self.launcher.start()
		self.set_button(True)
		self.set_ev_area(0, 0, runtime.app_width, screen_height)

	def init_score_labels(self):
		self.score = 0
		text = _('Score :')
		label = Text(self, self.screen, text, pos=(5, screen_height-28))
		self.score_label = Text(self, self.screen, str(self.score), pos=(65, screen_height-28))

		text = _('Hi-Score :')
		label = Text(self, self.screen, text, pos=(5, screen_height-10))
		self.hiscore_label = Text(self, self.screen, str(self.db['hi_score']), pos=(65, screen_height-10))
	
	def handle_button_event(self, ev):
		if ev != 'press' or self.ball:
			return
		if self.game_state == 'gameover':
			return
		elif self.game_state == 'idle':
			self.new_game()
			return

		self.launcher.stop()
		self.ball = ball = self.launcher.get_ball()
		if not ball:
			return
		def _handle_ball_expand_finished(ball):
			objects = self.shapes['objects']
			objects.append(ball)
			self.ball = None
			self.launcher.start()
		ball.add_callback('expand-finish', _handle_ball_expand_finished)

		def _handle_fall_down(ball, move_vector):
			self.increase_score()
			objects = self.shapes['objects']
			objects.remove(ball)

			def _finished():
				self.fall_down_action = None
			self.fall_down_action = FallDownAction(_finished)
			self.fall_down_action.run(ball, move_vector)

		ball.add_callback('fall-down', _handle_fall_down)

		def _destroyed(ball):
			objects = self.shapes['objects']
			if ball in objects:
				objects.remove(ball)
		ball.add_callback('destroy', _destroyed)

		angle = self.launcher.angle
		if not self.move_action:
			self.move_action = MoveAction(self.shapes, DEFAULT_SPEED,
							angle, self.handle_move_action_event)
		else:
			self.move_action.reset(DEFAULT_SPEED, angle)
		self.move_action.run(ball)

	def handle_move_action_event(self, ev, *args):
		if ev == 'gameover':
			color, pos = args
			self.game_state = 'gameover'
			def _timeout():
				self.timeout_tag = None
				self.game_state = 'idle'
				def _cb():
					self.new_game()
				from gameover_popup import GameOverPopup
				self.dlg = GameOverPopup(self.score)
				self.dlg.add_callback('dialog-close', _cb)
			self.timeout_tag = eloop.Timer(1000, _timeout)
			self.do_collision_effect(color, color, pos)
		elif ev == 'ball-contact':
			color1, color2, pos = args
			self.do_collision_effect(color1, color2, pos)

	def do_collision_effect(self, color1, color2, pos):
		fragment_num = 8
		import random
		for i in range(fragment_num):
			size = random.randint(4, 10)
			color = (color1, color2)[i%2]
			angle = i * (360 / fragment_num)
			fragment = Fragment(self, self.screen, size, angle, color, pos=pos)
			move_vector = vector.Vector((1, 0)).rotate(i * 45)
			def _finished(fragment):
				del self.fragments[fragment]
				fragment.destroy()
			fall_down_action = FragmentFallDownAction(_finished)
			fall_down_action.run(fragment, move_vector)
			self.fragments[fragment] = fall_down_action

	def set_score(self, score):
		self.score = score
		self.score_label.text = str(score)
		
		if self.db['hi_score'] < score:
			self.db['hi_score'] = score
			self.hiscore_label.text = str(score)

	def increase_score(self):
		self.set_score(self.score + 1)

	def hide(self):
		self.screen.hide()
		self.__super.hide()

	def show(self):
		self.screen.show()
		self.__super.show()

	def new_game(self):
		if self.move_action:
			self.move_action.destroy()
			self.move_action = None
		if self.fall_down_action:
			self.fall_down_action.destroy()
			self.fall_down_action = None

		objects = self.shapes['objects']
		for obj in objects[:]:
			obj.destroy()
		if self.ball:
			self.ball.destroy()
			self.ball = None
		self.shapes['objects'] = []
		self.game_state = 'playing'
		self.set_score(0)
		self.launcher.start()

	def handle_key(self, ev):
		down = ev.down
		key = ev.key
		if down and key in (keysym.Ok, keysym.OkChar):
			self.new_game()
			return True
		return self.__super.handle_key(ev)

	def close(self):
		if self.move_action:
			self.move_action.destroy()
		for fragment, action in self.fragments.items():
			action.destroy()
		self.__super.close()

class BumpingBallLoadingWindow(pyapp.Window):
	def __init__(self, app):
		self.__super.__init__(app)
		self.set_screen_type('fullscreen')
		bg_img = config.img_dir + 'bumpingball/main.png'
		from pui import canvas
		self.bg = canvas.XGImage(self, bg_img)

class BumpingBallApp(pyapp.PyApp):
	name = 'bumpingball'
	def __init__(self, *args, **kwds):
		pyapp.PyApp.__init__(self)

		setup_db = config.setup_dir + 'bumpingball.db'
		setup_init_db = config.prizm_app_dir + 'src/game/bumpingball/setup.init.db'

		from psys import param
		self.db = param.ParamPool(setup_db, setup_init_db)

		win = BumpingBallLoadingWindow(self)
		self.add_window(win)
		self.init_screen()

	def init_screen(self):
		def _init():
			pos = 0, 0
			size = runtime.app_width, screen_height
			bg = config.img_dir + 'bumpingball/game_bg.png'
			self.screen = BumpingBallScreen(pos, size, bg)

			win = BumpingBallWindow(self)
			self.replace_window(win)
			self._idle_tag = None
		self._idle_tag = eloop.Timer(1000, _init)

	def destroy(self):
		self.screen.destroy()
		pyapp.PyApp.destroy(self)
