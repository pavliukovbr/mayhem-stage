#!/usr/bin/env python3
"""Gera blockstates e models JSON dos blocos do mod (cube, pillar, stairs, slab, wall)."""
import json, os

A = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources", "assets", "mayhem")
BS, M = os.path.join(A,"blockstates"), os.path.join(A,"models","block")
os.makedirs(BS, exist_ok=True); os.makedirs(M, exist_ok=True)

def w(d, name, obj):
    with open(os.path.join(d, name+".json"), "w") as f: json.dump(obj, f, indent=1)

def cube(name, tex):
    w(M, name, {"parent":"minecraft:block/cube_all","textures":{"all":f"mayhem:block/{tex}"}})
    w(BS, name, {"variants":{"":{"model":f"mayhem:block/{name}"}}})

def pillar(name, side, end):
    w(M, name, {"parent":"minecraft:block/cube_column","textures":{"side":f"mayhem:block/{side}","end":f"mayhem:block/{end}"}})
    w(BS, name, {"variants":{
        "axis=y":{"model":f"mayhem:block/{name}"},
        "axis=x":{"model":f"mayhem:block/{name}","x":90,"y":90},
        "axis=z":{"model":f"mayhem:block/{name}","x":90}}})

def stairs(name, tex):
    t = {"bottom":f"mayhem:block/{tex}","top":f"mayhem:block/{tex}","side":f"mayhem:block/{tex}"}
    for suf,parent in (("","minecraft:block/stairs"),("_inner","minecraft:block/inner_stairs"),("_outer","minecraft:block/outer_stairs")):
        w(M, name+suf, {"parent":parent,"textures":t})
    v={}
    Y={"east":0,"west":180,"south":90,"north":270}
    for facing,y in Y.items():
        for half,x in (("bottom",0),("top",180)):
            uv = x==180
            for shape,(mdl,dy) in {"straight":(name,0),"inner_left":(name+"_inner",-90),
                                   "inner_right":(name+"_inner",0),"outer_left":(name+"_outer",-90),
                                   "outer_right":(name+"_outer",0)}.items():
                yy=(y+dy)%360
                if half=="top" and shape in ("inner_left","outer_left"): yy=(y)%360
                if half=="top" and shape in ("inner_right","outer_right"): yy=(y+90)%360
                ent={"model":f"mayhem:block/{mdl}"}
                if x: ent["x"]=x
                if yy: ent["y"]=yy
                if x or yy: ent["uvlock"]=True
                v[f"facing={facing},half={half},shape={shape}"]=ent
    w(BS, name, {"variants":v})

def slab(name, tex, full):
    t={"bottom":f"mayhem:block/{tex}","top":f"mayhem:block/{tex}","side":f"mayhem:block/{tex}"}
    w(M, name, {"parent":"minecraft:block/slab","textures":t})
    w(M, name+"_top", {"parent":"minecraft:block/slab_top","textures":t})
    w(BS, name, {"variants":{
        "type=bottom":{"model":f"mayhem:block/{name}"},
        "type=top":{"model":f"mayhem:block/{name}_top"},
        "type=double":{"model":f"mayhem:block/{full}"}}})

def wall(name, tex):
    t={"wall":f"mayhem:block/{tex}"}
    w(M, name+"_post", {"parent":"minecraft:block/template_wall_post","textures":t})
    w(M, name+"_side", {"parent":"minecraft:block/template_wall_side","textures":t})
    w(M, name+"_side_tall", {"parent":"minecraft:block/template_wall_side_tall","textures":t})
    parts=[{"when":{"up":"true"},"apply":{"model":f"mayhem:block/{name}_post"}}]
    for d,y in (("north",0),("east",90),("south",180),("west",270)):
        for h,mdl in (("low",f"{name}_side"),("tall",f"{name}_side_tall")):
            ap={"model":f"mayhem:block/{mdl}","uvlock":True}
            if y: ap["y"]=y
            parts.append({"when":{d:h},"apply":ap})
    w(BS, name, {"multipart":parts})

cube("ivory_plain","ivory_plain")
cube("ivory_carved","ivory_carved")
pillar("ivory_fluted","ivory_fluted_side","ivory_fluted_end")
cube("gloss_black","gloss_black")
cube("velvet_black","velvet_black")
cube("led_white","led_white")
cube("led_red","led_red")
cube("gold_leaf","gold_leaf")
stairs("ivory_stairs","ivory_plain")
slab("ivory_slab","ivory_plain","ivory_plain")
wall("ivory_wall","ivory_plain")
print("assets:", len(os.listdir(BS)), "blockstates,", len(os.listdir(M)), "models")
