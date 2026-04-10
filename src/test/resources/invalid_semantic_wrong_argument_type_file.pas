program WrongArgTypeFile;
var
  x: integer;

function NeedsBool(flag: boolean): integer;
begin
  if flag then
    NeedsBool := 1
  else
    NeedsBool := 0;
end;

begin
  x := NeedsBool(1);
end.
