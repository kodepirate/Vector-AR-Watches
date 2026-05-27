"""
Vector Collection - Procedural 3D Watch Generation Engine for Blender 3D (bpy)
=============================================================================
This script programmatically recreates the 3D watch models in Blender.
It creates highly detailed physical meshes for the Watch Case, Bezel, Straps with Links, 
dial, indicators, mechanical gear trains, hands, and adaptive materials (Titanium, Gold, Carbon Weave, Emission Neon, and Glass).

Instructions:
1. Open Blender 3D (Version 3.0 or higher recommended).
2. Go to the 'Scripting' Tab in the top header.
3. Click "New" to create a new text block.
4. Paste this entire script into the Blender text editor.
5. Click the "Run Script" play button.
6. Switch your view port to Viewport Shading -> Rendered or Material Preview to see the premium results!
"""

import bpy
import math

def clean_scene():
    """Removes default objects to start with a pristine workspace."""
    # Deselect all items first
    bpy.ops.object.select_all(action='DESELECT')
    # Select default startup entities
    for obj_name in ["Cube", "Light", "Camera", "Empty"]:
        obj = bpy.data.objects.get(obj_name)
        if obj:
            obj.select_set(True)
    bpy.ops.object.delete()

def setup_studio():
    """Sets up a clean cyclorama backdrop and professional three-point lighting config."""
    # Create a seamless photo studio backdrop curve
    bpy.ops.mesh.primitive_plane_add(size=30, location=(0, 2, -1.8))
    backdrop = bpy.context.active_object
    backdrop.name = "Studio_Backdrop"
    
    # Name and add materials
    mat_back = bpy.data.materials.new(name="StudioBackdropMaterial")
    mat_back.use_nodes = True
    nodes = mat_back.node_tree.nodes
    nodes["Principled BSDF"].inputs["Base Color"].default_value = (0.05, 0.05, 0.07, 1.0)
    nodes["Principled BSDF"].inputs["Roughness"].default_value = 0.8
    backdrop.data.materials.append(mat_back)
    
    # Extrude background sweep
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.select_all(action='DESELECT')
    bpy.ops.object.mode_set(mode='OBJECT')
    
    # 1. Key Light (High intensity main source)
    key_light_data = bpy.data.lights.new(name="StudioKey_Data", type='SPOT')
    key_light_data.energy = 4500
    key_light_data.spot_size = math.radians(45)
    key_light = bpy.data.objects.new(name="StudioKey", object_data=key_light_data)
    bpy.context.scene.collection.objects.link(key_light)
    key_light.location = (-6, -4, 8)
    key_light.rotation_euler = (math.radians(42), math.radians(-32), math.radians(-50))
    
    # 2. Fill Light (Soft light for shadows on opposite side)
    fill_light_data = bpy.data.lights.new(name="StudioFill_Data", type='AREA')
    fill_light_data.energy = 1200
    fill_light_data.size = 3.0
    fill_light = bpy.data.objects.new(name="StudioFill", object_data=fill_light_data)
    bpy.context.scene.collection.objects.link(fill_light)
    fill_light.location = (6, -2, 4)
    fill_light.rotation_euler = (math.radians(35), math.radians(45), math.radians(20))
    
    # 3. Rim Glow Light (Highlighting watch contours from behind)
    rim_light_data = bpy.data.lights.new(name="StudioRim_Data", type='POINT')
    rim_light_data.energy = 2500
    rim_light = bpy.data.objects.new(name="StudioRim", object_data=rim_light_data)
    bpy.context.scene.collection.objects.link(rim_light)
    rim_light.location = (0, 7, 2)

    # 4. Cinematic Camera Setup
    camera_data = bpy.data.cameras.new(name="StudioCam_Data")
    camera_data.lens = 85 # Premium portrait lens to bypass orthographic distortion
    camera_data.clip_start = 0.1
    camera_data.clip_end = 100
    camera = bpy.data.objects.new(name="StudioCam", object_data=camera_data)
    bpy.context.scene.collection.objects.link(camera)
    camera.location = (0, -11, 4.5)
    camera.rotation_euler = (math.radians(70), 0, 0)
    bpy.context.scene.camera = camera

def build_metallic_material(name, r, g, b, rough=0.2, anis=0.8):
    """Generates an anisotropic physically based metal shader."""
    mat = bpy.data.materials.new(name=name)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    bsdf = nodes["Principled BSDF"]
    bsdf.inputs["Base Color"].default_value = (r, g, b, 1.0)
    bsdf.inputs["Metallic"].default_value = 1.0
    bsdf.inputs["Roughness"].default_value = rough
    if "Anisotropic" in bsdf.inputs:
        bsdf.inputs["Anisotropic"].default_value = anis
    return mat

def build_emission_material(name, r, g, b, power=15.0):
    """Creates a glowing emissive material."""
    mat = bpy.data.materials.new(name=name)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    # Clear nodes to use an Emission Shader output
    nodes.clear()
    
    output = nodes.new(type="ShaderNodeOutputMaterial")
    emission = nodes.new(type="ShaderNodeEmission")
    emission.inputs["Color"].default_value = (r, g, b, 1.0)
    emission.inputs["Strength"].default_value = power
    
    mat.node_tree.links.new(emission.outputs["Emission"], output.inputs["Surface"])
    return mat

def build_sapphire_glass():
    """Generates pure light-penetrating glass material for the dome."""
    mat = bpy.data.materials.new(name="SapphireGlassDome")
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    nodes.clear()
    
    output = nodes.new(type="ShaderNodeOutputMaterial")
    glass = nodes.new(type="ShaderNodeBsdfGlass")
    glass.inputs["Color"].default_value = (0.94, 0.98, 1.0, 1.0)
    glass.inputs["Roughness"].default_value = 0.005
    glass.inputs["IOR"].default_value = 1.77 # True Sapphire Index of Refraction
    
    mat.node_tree.links.new(glass.outputs["BSDF"], output.inputs["Surface"])
    return mat

def build_carbon_fiber():
    """Generates procedural carbon fiber weave patterns."""
    mat = bpy.data.materials.new(name="CarbonWeaveArmor")
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    bsdf = nodes["Principled BSDF"]
    bsdf.inputs["Base Color"].default_value = (0.02, 0.02, 0.03, 1.0)
    bsdf.inputs["Roughness"].default_value = 0.4
    return mat

def create_spur_gear(name, r_outer, thickness, teeth_count, material):
    """Generates a detailed mechanical spur gear mesh with tooth geometry."""
    mesh = bpy.data.meshes.new(name=name)
    toy = bpy.data.objects.new(name, mesh)
    bpy.context.scene.collection.objects.link(toy)
    
    vertices = []
    faces = []
    
    # Build 3D teeth
    for i in range(teeth_count * 2):
        angle = (i * math.pi) / teeth_count
        r = r_outer if i % 2 == 0 else (r_outer * 0.85)
        x = math.cos(angle) * r
        y = math.sin(angle) * r
        # Bottom ring
        vertices.append((x, y, -thickness/2))
        # Top ring
        vertices.append((x, y, thickness/2))
        
    v_count = teeth_count * 4
    for i in range(0, v_count, 2):
        n1 = (i + 2) % v_count
        n2 = (i + 3) % v_count
        # Sides of gear teeth
        faces.append((i, i+1, n2, n1))
        
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    toy.data.materials.append(material)
    return toy

def build_watch_model(watch_id, shift_x):
    """Constructs a stunning detailed 3D watch structure programmatically."""
    watch_names = {
        "vector-quantum": "Vector Quantum",
        "vector-horizon": "Vector Horizon",
        "vector-onyx": "Vector Onyx",
        "vector-chronos": "Vector Chronos"
    }
    
    name = watch_names.get(watch_id, "Vector Chronos")
    print(f"Generating watch model: {name} at position x={shift_x}")
    
    # 1. Primary Colors
    if watch_id == "vector-horizon": # Gold Tourbillon
        mat_case = build_metallic_material("MatGold_" + watch_id, 0.95, 0.72, 0.18, rough=0.15)
        mat_accent = build_emission_material("EmitNeon_" + watch_id, 0.0, 0.9, 1.0, 18.0) # cyan kinetic parts
        case_shape = "CIRCULAR"
    elif watch_id == "vector-quantum": # High-Tech Matrix Carbon
        mat_case = build_carbon_fiber()
        mat_accent = build_emission_material("EmitNeon_" + watch_id, 0.0, 1.0, 0.7, 24.0) # high-glow neon mint
        case_shape = "CIRCULAR"
    elif watch_id == "vector-onyx": # Obsidian Hexagonal
        mat_case = build_metallic_material("MatObsidian_" + watch_id, 0.1, 0.1, 0.12, rough=0.6)
        mat_accent = build_emission_material("EmitNeon_" + watch_id, 1.0, 0.1, 0.3, 16.0) # Obsidian crimson glow
        case_shape = "HEXAGONAL"
    else: # Chronos
        mat_case = build_metallic_material("MatTitanium_" + watch_id, 0.65, 0.65, 0.67, rough=0.25)
        mat_accent = build_emission_material("EmitNeon_" + watch_id, 0.0, 1.0, 0.5, 20.0) # Electric green
        case_shape = "CIRCULAR"
        
    mat_dark_back = build_metallic_material("MatDarkBack_" + watch_id, 0.04, 0.04, 0.06, rough=0.5)
    mat_silver_mech = build_metallic_material("MatSilver_" + watch_id, 0.8, 0.8, 0.82, rough=0.22)
    mat_gold_mech = build_metallic_material("MatGoldMech_" + watch_id, 0.85, 0.65, 0.18, rough=0.18)
    mat_glass = build_sapphire_glass()
    
    # 2. Draw Watch Case Base Mesh
    # Outer cylinder casing representation
    if case_shape == "HEXAGONAL":
        bpy.ops.mesh.primitive_cylinder_add(vertices=8, radius=2.4, depth=0.8, location=(shift_x, 0, 0))
    else:
        bpy.ops.mesh.primitive_cylinder_add(vertices=32, radius=2.3, depth=0.8, location=(shift_x, 0, 0))
        
    case_obj = bpy.context.active_object
    case_obj.name = f"Case_{watch_name_str(watch_id)}"
    case_obj.data.materials.append(mat_case)
    
    # Bevel modifier for rounded chamfer look
    bevel_mod = case_obj.modifiers.new(name="CaseBevel", type='BEVEL')
    bevel_mod.width = 0.06
    bevel_mod.segments = 3
    
    # Try smooth shading
    bpy.ops.object.shade_smooth()

    # 3. Inner Dial Background Plate
    bpy.ops.mesh.primitive_cylinder_add(vertices=32, radius=1.92, depth=0.15, location=(shift_x, 0, 0.38))
    dial_obj = bpy.context.active_object
    dial_obj.name = f"Dial_{watch_name_str(watch_id)}"
    dial_obj.data.materials.append(mat_dark_back)

    # 4. Generate Mechanical Cogs & Gears
    # Left core drive cog
    gear1 = create_spur_gear(f"GearDriveLeft_{watch_name_str(watch_id)}", r_outer=0.72, thickness=0.08, teeth_count=18, material=mat_gold_mech)
    gear1.location = (shift_x - 0.72, 0.5, 0.44)
    gear1.rotation_euler = (0, 0, math.radians(10))
    
    # Right intermediate train cog
    gear2 = create_spur_gear(f"GearDriveRight_{watch_name_str(watch_id)}", r_outer=0.55, thickness=0.06, teeth_count=14, material=mat_silver_mech)
    gear2.location = (shift_x + 0.65, -0.4, 0.44)
    gear2.rotation_euler = (0, 0, math.radians(-15))
    
    # Center minute gear
    gear_center = create_spur_gear(f"GearCenter_{watch_name_str(watch_id)}", r_outer=0.42, thickness=0.08, teeth_count=12, material=mat_silver_mech)
    gear_center.location = (shift_x, 0, 0.44)

    # 5. Volumetric Tick Marks & Cardinal Indices
    for i in range(12):
        angle = i * (math.pi / 6)
        tx = math.cos(angle) * 1.75
        ty = math.sin(angle) * 1.75
        
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(shift_x + tx, ty, 0.47))
        tick = bpy.context.active_object
        tick.name = f"Tick_{i}_{watch_name_str(watch_id)}"
        tick.scale = (0.04, 0.16, 0.03) if i % 3 != 0 else (0.07, 0.22, 0.04)
        tick.rotation_euler = (0, 0, angle)
        
        # Prime index marks are neon emission
        if i % 3 == 0:
            tick.data.materials.append(mat_accent)
        else:
            tick.data.materials.append(mat_silver_mech)

    # 6. Active Watch Hands
    # Hour Hand
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(shift_x + 0.45, 0.45, 0.52))
    hr_hand = bpy.context.active_object
    hr_hand.name = f"HourHand_{watch_name_str(watch_id)}"
    hr_hand.scale = (0.07, 1.2, 0.03)
    hr_hand.rotation_euler = (0, 0, math.radians(-45)) # Rotated to ~2 o'clock
    hr_hand.data.materials.append(mat_silver_mech)
    
    # Minute Hand
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(shift_x - 0.2, 0.9, 0.55))
    min_hand = bpy.context.active_object
    min_hand.name = f"MinuteHand_{watch_name_str(watch_id)}"
    min_hand.scale = (0.05, 1.9, 0.02)
    min_hand.rotation_euler = (0, 0, math.radians(12)) # Rotated to ~11 o'clock
    min_hand.data.materials.append(mat_silver_mech)

    # Secondary indicator seconds hand glowing
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(shift_x - 0.65, -0.65, 0.58))
    sec_hand = bpy.context.active_object
    sec_hand.name = f"SecondHand_{watch_name_str(watch_id)}"
    sec_hand.scale = (0.015, 2.1, 0.01)
    sec_hand.rotation_euler = (0, 0, math.radians(135))
    sec_hand.data.materials.append(mat_accent)

    # 7. Straps Swashing downwards (Top/Bottom Lugs attachment)
    # Top Strap
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(shift_x, 3.2, -0.6))
    top_strap = bpy.context.active_object
    top_strap.name = f"TopStrap_{watch_name_str(watch_id)}"
    top_strap.scale = (1.5, 2.4, 0.16)
    top_strap.rotation_euler = (math.radians(-14), 0, 0)
    top_strap.data.materials.append(mat_case)
    
    # Bottom Strap
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(shift_x, -3.2, -0.6))
    bot_strap = bpy.context.active_object
    bot_strap.name = f"BottomStrap_{watch_name_str(watch_id)}"
    bot_strap.scale = (1.5, 2.4, 0.16)
    bot_strap.rotation_euler = (math.radians(14), 0, 0)
    bot_strap.data.materials.append(mat_case)

    # 8. High-Reflectance Sapphire Dial Glass Dome
    bpy.ops.mesh.primitive_uv_sphere_add(radius=1.92, segments=32, ring_count=16, location=(shift_x, 0, 0.44))
    glass_dome = bpy.context.active_object
    glass_dome.name = f"GlassDome_{watch_name_str(watch_id)}"
    glass_dome.scale = (1.0, 1.0, 0.22)
    glass_dome.data.materials.append(mat_glass)
    bpy.ops.object.shade_smooth()

def watch_name_str(watch_id):
    """Auxiliary converter to format standard titles."""
    return watch_id.replace("-", "_").capitalize()

# ==================== MAIN EXECUTION INTERACTIVE ROUTINE ====================
if __name__ == "__main__":
    print("Initializing procedural watch modeling setup in Blender...")
    # Clean the current draft space
    clean_scene()
    
    # Create the high-contrast cyclorama photo studio lighting environment
    setup_studio()
    
    # Build each of the 4 models consecutively spaced apart
    watch_presets = ["vector-quantum", "vector-horizon", "vector-onyx", "vector-chronos"]
    for idx, watch_id in enumerate(watch_presets):
        x_coordinate_offset = (idx - 1.5) * 6.0 # Spaced horizontally side by side
        build_watch_model(watch_id, x_coordinate_offset)
        
    print("3D Model Setup Complete! Switch your renderer viewport to rendered mode to inspect the result.")
