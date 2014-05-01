skipTests=false
mvn_exec=mvn -Dmaven.test.skip=${skipTests}

help:
	@echo
	@echo "Mica Server"
	@echo
	@echo "Available make targets:"
	@echo "  all         : Clean & install all modules"
	@echo "  core        : Install core module"
	@echo "  rest        : Install rest module"
	@echo
	@echo "  run         : Run webapp module"
	@echo "  debug       : Debug webapp module on port 8000"
	@echo "  grunt       : Start grunt on port 9000"
	@echo "  npm-install : Download all NodeJS dependencies"
	@echo
	@echo "  clear-log   : Delete mica.log from mica-webapp/target"
	@echo "  drop-mongo  : Drop MongoDB mica database"
	@echo
	@echo "  dependencies-tree   : Displays the dependency tree"
	@echo "  dependencies-update : Check for new dependency updates"
	@echo "  plugins-update      : Check for new plugin updates"
	@echo

all:
	${mvn_exec} clean install

core:
	cd mica-core && ${mvn_exec} install

rest:
	cd mica-rest && ${mvn_exec} install

run:
	cd mica-webapp && ${mvn_exec} spring-boot:run

debug:
	export MAVEN_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n && \
	cd mica-webapp && ${mvn_exec} spring-boot:run

grunt:
	cd mica-webapp && grunt server

npm-install:
	cd mica-webapp && npm install

clear-log:
	rm -f mica-webapp/target/mica.log*

drop-mongo:
	mongo mica --eval "db.dropDatabase()"

dependencies-tree:
	mvn dependency:tree

dependencies-update:
	mvn versions:display-dependency-updates

plugins-update:
	mvn versions:display-plugin-updates