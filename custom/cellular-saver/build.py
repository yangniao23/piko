#!/usr/bin/env python3
"""Build the local MPP; Python 3 and JDK 21 required. No upload or installation."""
import argparse, pathlib, subprocess, shutil, zipfile
p=argparse.ArgumentParser()
p.add_argument('--jdk',required=True,help='JDK 21 directory')
p.add_argument('--morphe',required=True,help='morphe-desktop-1.15.0-all.jar')
p.add_argument('--r8',required=True,help='r8-8.3.37.jar')
a=p.parse_args()
root=pathlib.Path(__file__).resolve().parent
jdk=pathlib.Path(a.jdk).resolve(); morphe=pathlib.Path(a.morphe).resolve(); r8=pathlib.Path(a.r8).resolve()
build=root/'build'; bundle=build/'bundle'; dist=root/'dist'
for d in (build,bundle/'extensions',build/'dex',dist): d.mkdir(parents=True,exist_ok=True)
def run(tool,*args):subprocess.run([str(jdk/'bin'/tool),*map(str,args)],check=True,cwd=root)
run('javac','-cp',morphe,'-d',build,root/'src/Assemble.java',root/'src/CellularSaverPatch.java')
run('java','-cp',str(morphe)+__import__('os').pathsep+str(build),'Assemble',root/'smali',bundle/'extensions/cellular-saver.dex')
shutil.copytree(build/'mobile',bundle/'mobile',dirs_exist_ok=True)
run('jar','cf',build/'patch.jar','-C',bundle,'mobile')
run('java','-cp',r8,'com.android.tools.r8.D8','--min-api','26','--lib',jdk,'--classpath',morphe,'--output',build/'dex',build/'patch.jar')
shutil.copy2(build/'dex/classes.dex',bundle/'classes.dex')
run('jar','cfm',dist/'cellular-saver-12.19.1.mpp',root/'manifest.mf','-C',bundle,'.')
run('java','-jar',morphe,'list-patches','--patches',dist/'cellular-saver-12.19.1.mpp','-pv')
