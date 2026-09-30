# Projectile preview

Enable **Right Shift → RENDER → Projectile preview**. While holding an ender pearl,
snowball, egg, splash potion, or lingering potion, the client draws an estimated
flight path. For bows, hold use to draw the bow first; the path grows with draw power.
The line stops at the first predicted block or entity hit. Orange marks a block,
red marks an entity, and a green marker briefly shows where one of your projectiles
actually hit.

The preview starts disabled and lasts only for the current client session. It uses
local position, aim, motion, gravity, drag, and collision data; projectile spread,
server latency, and entities moving after the throw can change the real impact.
