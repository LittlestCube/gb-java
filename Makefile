all: submodule dev
	jar cvfe Gameboy.jar GB *
	$(MAKE) clean-leavejar

dev:
	javac *.java

run:
	java GB

submodule: clean
	git submodule update --init --recursive --remote --merge
	git submodule foreach git pull origin master
	cp -r unsigned/littlecube littlecube
	cp -r bitutil/littlecube/bitutil littlecube/bitutil

clean: clean-leavejar
	rm *.jar || continue

clean-leavejar:
	rm *.class || continue
	rm -r littlecube || continue